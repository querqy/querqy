package querqy.lucene.rewrite;

import querqy.lucene.rewrite.BooleanQueryFactory.Clause;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>A post-processor of a structure of {@link LuceneQueryFactory}s that wraps a
 * {@link org.apache.lucene.search.DisjunctionMaxQuery} around clauses of DisjunctionMaxQuerys that were created from
 * the same input token.</p>
 * <p>Let's assume an input query 'asus laptop' and a synonym expansion 'notebook' for laptop, and two query fields
 * f1 and f2. This gives us DisMax(f1:asus | f2:asus) and DisMax(f1:laptop|f2:laptop|f1:notebook|f2:notebook) as DisMax
 * queries, each wrapping the term queries that were derived from a common input token. A second level of grouping is
 * applied per token value. This time we wrap a DisMax query around token values so that the disjunction-maximization of
 * scores works between fields: DisMax(DisMax(f1:laptop|f2:laptop)|DisMax(f1:notebook|f2:notebook))</p>
 * <p>Each grouping level comes with its own tie parameter, where the per-value grouping (dmqTieBreakerMultiplier)
 * corresponds to the tie parameter as it is known from Lucene's dismax, and where the multiMatchTieBreakerMultiplier
 * is the tie for the newly introduced grouping per input term:
 * <pre>
 *     DisMax(f1:asus | f2:asus)~dmqTieBreakerMultiplier
 *     DisMax(
 *          DisMax(f1:laptop|f2:laptop)~dmqTieBreakerMultiplier
 *          |
 *          DisMax(f1:notebook|f2:notebook)~dmqTieBreakerMultiplier
 *     )~multiMatchTieBreakerMultiplier
 * </pre>
 *
 */
public class MultiMatchDismaxQueryStructurePostProcessor extends LuceneQueryFactoryVisitor<Void> {

    private final float dmqTieBreakerMultiplier;
    private final float multiMatchTieBreakerMultiplier;

    /**
     *
     * @param dmqTieBreakerMultiplier tiebreaker for per-value grouping
     * @param multiMatchTieBreakerMultiplier - tiebreaker for grouping by input term
     */
    public MultiMatchDismaxQueryStructurePostProcessor(final float dmqTieBreakerMultiplier,
                                                       final float multiMatchTieBreakerMultiplier) {
        this.dmqTieBreakerMultiplier = dmqTieBreakerMultiplier;
        this.multiMatchTieBreakerMultiplier = multiMatchTieBreakerMultiplier;
    }

    @Override
    public Void visit(final BooleanQueryFactory factory) {
        factory.getClauses().forEach(clause -> applyMultiMatch(clause.queryFactory));
        return null;
    }

    @Override
    public Void visit(final DisjunctionMaxQueryFactory factory) {
        super.visit(factory);
        factory.disjuncts.forEach(this::applyMultiMatch);
        return null;
    }

    @Override
    public Void visit(final TermSubQueryFactory factory) {
        return null;
    }

    /**
     * Rewrite the query structure. The operation might manipulate the input query structure and not operate on a copy.
     *
     * @param structure The input query structure.
     * @return The rewritten structure.
     */
    public LuceneQueryFactory<?> process(final LuceneQueryFactory<?> structure) {
        if (structure instanceof DisjunctionMaxQueryFactory) {
            processTopLevelDmq((DisjunctionMaxQueryFactory) structure);
        } else {
            structure.accept(this);
        }
        return structure;
    }

    protected void applyMultiMatch(final LuceneQueryFactory<?> structure) {
        TopLevelDmqFinder.findDmqs(structure).forEach(this::processTopLevelDmq);
    }

    protected void processTopLevelDmq(final DisjunctionMaxQueryFactory dmq) {
        final int numDisjuncts = dmq.getNumberOfDisjuncts();
        if (numDisjuncts > 1) {

            final Map<String, List<LuceneQueryFactory<?>>> factoriesByField = RegroupDisjunctsByFieldProcessor
                    .regroupByFields(dmq.disjuncts);

            dmq.disjuncts.clear();

            for (final Map.Entry<String, List<LuceneQueryFactory<?>>> entry : factoriesByField.entrySet()) {
                final List<LuceneQueryFactory<?>> factories = entry.getValue();
                if (factories.size() == 1) {
                    dmq.add(factories.get(0));
                } else {
                    dmq.add(new DisjunctionMaxQueryFactory(factories, multiMatchTieBreakerMultiplier));
                }
            }
            dmq.setTieBreaker(dmqTieBreakerMultiplier);

        }
    }

    public static class RegroupDisjunctsByFieldProcessor extends LuceneQueryFactoryVisitor<Void> {

        public static Map<String, List<LuceneQueryFactory<?>>> regroupByFields(final List<LuceneQueryFactory<?>>
                                                                                       disjuncts) {

            final RegroupDisjunctsByFieldProcessor processor = new RegroupDisjunctsByFieldProcessor();
            disjuncts.forEach(factory -> factory.accept(processor));
            return processor.factoriesByField;

        }

        private final Map<String, List<LuceneQueryFactory<?>>> factoriesByField = new HashMap<>();

        private RegroupDisjunctsByFieldProcessor() {}

        public void visitTerm(final String fieldname, final LuceneQueryFactory<?> factory) {
            factoriesByField.computeIfAbsent(fieldname, k -> new ArrayList<>()).add(factory);
        }

        @Override
        public Void visit(final TermQueryFactory factory) {
            visitTerm(factory.getFieldname(), factory);
            return null;
        }

        @Override
        public Void visit(final TermSubQueryFactory factory) {
            visitTerm(factory.getFieldname(), factory);
            return null;
        }

        @Override
        public Void visit(final NeverMatchQueryFactory factory) {
            factoriesByField.computeIfAbsent(null, k -> Collections.singletonList(factory));
            return null;
        }

        @Override
        public Void visit(final DisjunctionMaxQueryFactory factory) {
            // pull up the disjuncts one level
            // FIXME: nested BooleanQuery?
            factory.disjuncts.forEach(disjunct -> disjunct.accept(this));
            return null;
        }

        @Override
        public Void visit(final BooleanQueryFactory factory) {

            final Set<String> fieldnames = FieldnameCollector.collectFieldnames(factory);

            switch (fieldnames.size()) {
                case 0: break;
                case 1: factoriesByField
                        .computeIfAbsent(fieldnames.iterator().next(), k -> new ArrayList<>())
                        .add(factory);
                    break;
                default:
                    for (final String fieldname : fieldnames) {
                        factoriesByField
                                .computeIfAbsent(fieldname, k -> new ArrayList<>())
                                .add(SingleFieldBoostCopy.copy(fieldname, factory));
                    }

            }

            return null;
        }
    }

    public static class SingleFieldBoostCopy extends LuceneQueryFactoryVisitor<LuceneQueryFactory<?>> {

        public static LuceneQueryFactory<?> copy(final String fieldname, final LuceneQueryFactory<?> factory) {
            return factory.accept(new SingleFieldBoostCopy(fieldname));
        }

        private final String fieldname;

        public SingleFieldBoostCopy(final String fieldname) {
            this.fieldname = fieldname;
        }

        public LuceneQueryFactory<?> visit(final BooleanQueryFactory factory) {

            final List<Clause> clausesCopy = factory.getClauses()
                    .stream()
                    .map(clause -> new Clause(clause.queryFactory.accept(this), clause.occur))
                    .collect(Collectors.toList());

            return new BooleanQueryFactory(clausesCopy, factory.normalizeBoost);

        }

        public LuceneQueryFactory<?> visit(final DisjunctionMaxQueryFactory factory) {

            final List<LuceneQueryFactory<?>> disjunctsCopy = factory.disjuncts.stream()
                    .map(disjunct -> (LuceneQueryFactory<?>) disjunct.accept(this))
                    .collect(Collectors.toList());

            return new DisjunctionMaxQueryFactory(disjunctsCopy, factory.tieBreaker);
        }

        public LuceneQueryFactory<?> visit(final TermSubQueryFactory factory) {

            final SingleFieldBoost singleFieldBoost = new SingleFieldBoost(fieldname, factory.boost);
            final LuceneQueryFactory<?> rootCopy = factory.root.accept(this);
            return new TermSubQueryFactory(rootCopy, factory.prmsQuery, singleFieldBoost, factory.getSourceTerm(),
                    factory.getFieldname());

        }
        public LuceneQueryFactory<?> visit(final TermQueryFactory factory) {
            return factory;
        }

        public LuceneQueryFactory<?> visit(final NeverMatchQueryFactory factory) {
            return factory;
        }

    }

    /**
     * Helper class to collect field names in a query tree.
     */
    public final static class FieldnameCollector extends LuceneQueryFactoryVisitor<Void> {

        public static Set<String> collectFieldnames(final LuceneQueryFactory<?> factory) {
            final FieldnameCollector collector = new FieldnameCollector();
            factory.accept(collector);
            return collector.fieldnames;
        }

        private final Set<String> fieldnames = new HashSet<>();

            @Override
            public Void visit(final TermSubQueryFactory factory) {
                fieldnames.add(factory.getFieldname());
                return null;
            }

            @Override
            public Void visit(final TermQueryFactory factory) {
                fieldnames.add(factory.getFieldname());
                return null;
            }

    }

    /**
     * Helper class to find the top-level {@link DisjunctionMaxQueryFactory}s in a query tree.
     */
    public final static class TopLevelDmqFinder extends LuceneQueryFactoryVisitor<Void> {

        /**
         * Static helper method.
         *
         * @param structure The tree structure from which to find the top-level {@link DisjunctionMaxQueryFactory}s
         *
         * @return The top-level {@link DisjunctionMaxQueryFactory}s, empty of there aren't any {@link DisjunctionMaxQueryFactory}s in the tree.
         */
        public static List<DisjunctionMaxQueryFactory> findDmqs(final LuceneQueryFactory<?> structure) {
            final TopLevelDmqFinder finder = new TopLevelDmqFinder();
            structure.accept(finder);
            return finder.dmqs;
        }

        final List<DisjunctionMaxQueryFactory> dmqs = new LinkedList<>();

        @Override
        public Void visit(final DisjunctionMaxQueryFactory factory) {
            dmqs.add(factory);
            // do not descend to clauses
            return null;
        }
    }
}
