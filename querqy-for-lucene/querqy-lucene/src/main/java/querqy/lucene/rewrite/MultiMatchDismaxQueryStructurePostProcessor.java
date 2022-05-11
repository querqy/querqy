package querqy.lucene.rewrite;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
        super.visit(factory);
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

            final Map<Object, List<LuceneQueryFactory<?>>> grouped = dmq.disjuncts.stream()
                    .collect(Collectors.groupingBy(this::getGroupingValue));

            grouped.values().stream()
                    .filter(factories -> (numDisjuncts > factories.size()) && factories.size() > 1)
                    .forEach(factories -> applyDisjunctGrouping(dmq, factories));

            if (numDisjuncts > dmq.getNumberOfDisjuncts()) {
                dmq.setTieBreaker(multiMatchTieBreakerMultiplier);
            }

        }
    }

    protected Object getGroupingValue(final LuceneQueryFactory<?> factory) {
        if (factory instanceof TermSubQueryFactory) {
            return ((TermSubQueryFactory) factory).getSourceTerm().getValue();
        } else if (factory instanceof TermQueryFactory) {
            return ((TermQueryFactory) factory).sourceTerm.getValue();
        } else return factory;
    }

    protected void applyDisjunctGrouping(final DisjunctionMaxQueryFactory dmq,
                                         final List<LuceneQueryFactory<?>> factories) {
        final DisjunctionMaxQueryFactory group = new DisjunctionMaxQueryFactory(factories, dmqTieBreakerMultiplier);
        factories.forEach(dmq.disjuncts::remove);
        dmq.disjuncts.add(group);
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
