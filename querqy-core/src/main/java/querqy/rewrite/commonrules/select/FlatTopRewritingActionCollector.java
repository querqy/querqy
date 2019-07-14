package querqy.rewrite.commonrules.select;

import querqy.PriorityComparator;
import querqy.rewrite.commonrules.model.Action;
import querqy.rewrite.commonrules.model.Instructions;

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FlatTopRewritingActionCollector implements TopRewritingActionCollector {

    private final TreeMap<Instructions, Function<Instructions, Action>> topN;
    private final int limit;
    private List<? extends FilterCriterion> filters;
    private final Comparator<Instructions> comparator;

    public FlatTopRewritingActionCollector(final List<Comparator<Instructions>> comparators, final int limit,
                                           final List<? extends FilterCriterion> filters) {
        comparator = new PriorityComparator<>(comparators);
        topN = new TreeMap<>(comparator);
        this.limit = limit;
        this.filters = filters;
    }


    @Override
    public void offer(final List<Instructions> instructions, final Function<Instructions, Action> actionCreator) {

        if (limit == 0) {
            return;
        }

        instructions.stream()
                .filter(instr -> {
                    for (final FilterCriterion filter : filters) {
                        if (!filter.isValid(instr)) {
                            return false;
                        }
                    }
                    return true;
                }).forEach( instr -> {

            if (limit < 0) {
                topN.put(instr, actionCreator);
            } else if (topN.size() < limit) {
                topN.put(instr, actionCreator);
            } else {
                final Instructions lastInstructions = topN.lastKey();
                if (comparator.compare(lastInstructions, instr) > 0) {
                    topN.put(instr, actionCreator);
                    if (topN.size() > limit) {
                        topN.remove(lastInstructions);
                    }
                }
            }

        });

    }

    @Override
    public List<Action> createActions() {

        return topN.entrySet().stream()
                .map(entry -> entry.getValue().apply(entry.getKey()))
                .collect(Collectors.toList());

    }

    @Override
    public int getLimit() {
        return limit;
    }

    @Override
    public List<? extends FilterCriterion> getFilters() {
        return filters;
    }

}
