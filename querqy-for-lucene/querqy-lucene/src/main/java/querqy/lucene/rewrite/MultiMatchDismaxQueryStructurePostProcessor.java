package querqy.lucene.rewrite;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MultiMatchDismaxQueryStructurePostProcessor {

    private final float dmqTieBreakerMultiplier;
    private final float multiMatchTieBreakerMultiplier;

    public MultiMatchDismaxQueryStructurePostProcessor(final float dmqTieBreakerMultiplier,
                                                       final float multiMatchTieBreakerMultiplier) {
        this.dmqTieBreakerMultiplier = dmqTieBreakerMultiplier;
        this.multiMatchTieBreakerMultiplier = multiMatchTieBreakerMultiplier;
    }

    public LuceneQueryFactory<?> process(final LuceneQueryFactory<?> structure) {

        for (final DisjunctionMaxQueryFactory dmq: TopLevelDmqFinder.findDmqs(structure)) {

            final int numDisjuncts = dmq.getNumberOfDisjuncts();
            if (numDisjuncts > 1) {

                final Map<Object, List<LuceneQueryFactory<?>>> grouped = dmq.disjuncts.stream()
                        .collect(Collectors.groupingBy(factory -> {
                    if (factory instanceof TermSubQueryFactory) {
                        return ((TermSubQueryFactory) factory).sourceTerm.getValue();
                    } else if (factory instanceof TermQueryFactory) {
                        return ((TermQueryFactory) factory).sourceTerm.getValue();
                    } else return factory;

                }));

                grouped.values().stream()
                        .filter(factories -> (numDisjuncts > factories.size()) && factories.size() > 1)
                        .forEach(factories -> {
                            final DisjunctionMaxQueryFactory group = new DisjunctionMaxQueryFactory(factories,
                            dmqTieBreakerMultiplier);
                            factories.forEach(dmq.disjuncts::remove);
                            dmq.disjuncts.add(group);
                });

                if (numDisjuncts > dmq.getNumberOfDisjuncts()) {
                    dmq.setTieBreaker(multiMatchTieBreakerMultiplier);
                }

            }


        }
        return structure;
    }

    public final static class TopLevelDmqFinder extends LuceneQueryFactoryVisitor<Void> {

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
