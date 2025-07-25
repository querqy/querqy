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
 * <p>A post-processor of a structure of {@link LuceneQueryFactory}s that groups clauses of a
 * {@link org.apache.lucene.search.DisjunctionMaxQuery} by fields. The fields are taken from the terms that are found
 * within the clauses, including descendants further down in the query tree. Each group will be wrapped in a new
 * DisjunctionMaxQuery so that all sub-queries under such a wrapper query relate to the same field. This allows us
 * to apply a tie breaker factor between fields and between the matches within a single field, which can be used to
 * avoid giving a high score to documents that match more than one query time synonym.</p>
 *
 <p>Let's assume an input query 'asus laptop' and a synonym expansion 'notebook' for laptop, and two query fields
 * f1 and f2. This gives us a BooleanQuery with two clauses: DisMax(f1:asus | f2:asus) and
 * DisMax(f1:laptop|f2:laptop|f1:notebook|f2:notebook), each of these DisMax queries wrapping the term queries that were
 * derived from a single input token. If a document now matched on notebook and laptop in, say, f2, it would receive a
 * greater score than documents that match on just oone of these terms.</p>
 * <p>In order to overcome this issue, we apply the grouping that we explained above and introduce
 * 'multiMatchTieBreakerMultiplier` as a new parameter:
 * <pre>
 *     DisMax(f1:asus | f2:asus)~dmqTieBreakerMultiplier
 *     DisMax(
 *          DisMax(f1:laptop|f1:notebook)~multiMatchTieBreakerMultiplier
 *          |
 *          DisMax(f2:laptop|f2:notebook)~multiMatchTieBreakerMultiplier
 *     )~dmqTieBreakerMultiplier
 * </pre>
 *
 * <p>Now multiMatchTieBreakerMultiplier allows us to control how scores are aggregated for the terms matching within the
 * same field and dmqTieBreakerMultiplier controls the aggregation across fields.
 * </p>
 * <p>If we want to group the clauses of a DisMaxQuery that contain a BooleanQuery (BQ), we will have to make sure that
 * the clauses of the BQ can still match across fields after the grouping that we described above. We apply the
 * following solution: Given synonyms 'PC = personal computer' as query
 * 'DisMax(f1:PC | f2:PC | BQ(DisMax(f1:personal | f2:personal), DisMax(f1:computer | f2:computer)' we produce the
 * following rewritten query:
 * <pre>
 *     DisMax(
 *          DisMax(f1:PC | BQ(
 *                              DisMax(f1:personal | f2:personal^0),
 *                              DisMax(f1:computer | f2:computer^0)
 *                            )
 *          )~multiMatchTieBreakerMultiplier
 *          |
 *          DisMax(f2:PC | BQ(
 *                              DisMax(f1:personal^0 | f2:personal),
 *                              DisMax(f1:computer^0 | f2:computer)
 *                            )
 *          )~multiMatchTieBreakerMultiplier
 *     )~dmqTieBreakerMultiplier
 *
 * </pre>
 * <p>i.e. we repeat the BQ in each field group but only accept the weight of the field of the group and set all other
 * field weights to 0. This structure allows us to match the clauses of the BQ across fields - just like in the original
 * query.
 * </p>
 * @author renekrie
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

        /**
         * Group {$link LuceneQueryFactory}s by the field names of the term queries contained.
         *
         * @param factories The factories
         * @return A mapping from field names to lists of factories.
         */
        public static Map<String, List<LuceneQueryFactory<?>>> regroupByFields(final List<LuceneQueryFactory<?>>
                                                                                       factories) {

            final RegroupDisjunctsByFieldProcessor processor = new RegroupDisjunctsByFieldProcessor();
            factories.forEach(factory -> factory.accept(processor));
            return processor.factoriesByField;

        }

        private final Map<String, List<LuceneQueryFactory<?>>> factoriesByField = new HashMap<>();

        private RegroupDisjunctsByFieldProcessor() {}

        /**
         * Collects the {@link LuceneQueryFactory} for a given fieldname into the factoriesByField map.
         * @param fieldname The field name
         * @param factory The factory
         */
        protected void collectFactoryForField(final String fieldname, final LuceneQueryFactory<?> factory) {
            factoriesByField.computeIfAbsent(fieldname, k -> new ArrayList<>()).add(factory);
        }

        @Override
        public Void visit(final TermQueryFactory factory) {
            collectFactoryForField(factory.getFieldname(), factory);
            return null;
        }

        @Override
        public Void visit(final TermSubQueryFactory factory) {
            collectFactoryForField(factory.getFieldname(), factory);
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

        /**
         * Copies a sub-query tree so that all field boosts will be set to 0 except for a single field, which will keep
         * its original boost.
         *
         * @param fieldname Keep boosts for this field
         * @param structure The root of the sub-query
         * @return The copy
         */
        public static LuceneQueryFactory<?> copy(final String fieldname, final LuceneQueryFactory<?> structure) {
            return structure.accept(new SingleFieldBoostCopy(fieldname));
        }

        private final String fieldname;

        public SingleFieldBoostCopy(final String fieldname) {
            this.fieldname = fieldname;
        }

        @Override
        public LuceneQueryFactory<?> visit(final BooleanQueryFactory factory) {

            final List<Clause> clausesCopy = factory.getClauses()
                    .stream()
                    .map(clause -> new Clause(clause.queryFactory.accept(this), clause.occur))
                    .collect(Collectors.toList());

            return new BooleanQueryFactory(clausesCopy, factory.normalizeBoost);

        }

        @Override
        public LuceneQueryFactory<?> visit(final DisjunctionMaxQueryFactory factory) {

            final List<LuceneQueryFactory<?>> disjunctsCopy = factory.disjuncts.stream()
                    .map(disjunct -> (LuceneQueryFactory<?>) disjunct.accept(this))
                    .collect(Collectors.toList());

            return new DisjunctionMaxQueryFactory(disjunctsCopy, factory.tieBreaker);
        }

        @Override
        public LuceneQueryFactory<?> visit(final TermSubQueryFactory factory) {

            final SingleFieldBoost singleFieldBoost = new SingleFieldBoost(fieldname, factory.boost);
            final LuceneQueryFactory<?> rootCopy = factory.root.accept(this);
            return new TermSubQueryFactory(rootCopy, factory.prmsQuery, singleFieldBoost, factory.getSourceTerm(),
                    factory.getFieldname());

        }

        @Override
        public LuceneQueryFactory<?> visit(final TermQueryFactory factory) {
            return factory;
        }

        @Override
        public LuceneQueryFactory<?> visit(final NeverMatchQueryFactory factory) {
            return factory;
        }

    }

    /**
     * Helper class to collect field names in a query tree.
     */
    public final static class FieldnameCollector extends LuceneQueryFactoryVisitor<Void> {

        /**
         * Collect all field names from a {@link LuceneQueryFactory} and its sub-queries.
         *
         * @param structure The root of the sub-query
         * @return The set of field names found
         */
        public static Set<String> collectFieldnames(final LuceneQueryFactory<?> structure) {
            final FieldnameCollector collector = new FieldnameCollector();
            structure.accept(collector);
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
