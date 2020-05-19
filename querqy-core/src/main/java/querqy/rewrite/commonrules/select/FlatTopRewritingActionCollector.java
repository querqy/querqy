package querqy.rewrite.commonrules.select;

import querqy.PriorityComparator;
import querqy.rewrite.commonrules.model.Action;
import querqy.rewrite.commonrules.model.Instructions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FlatTopRewritingActionCollector implements TopRewritingActionCollector {

    private final TreeMap<Instructions, List<Function<Instructions, Action>>> topN;
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
                collectEntry(instr, actionCreator);
            } else if (topN.size() < limit) {
                collectEntry(instr, actionCreator);
            } else {
                final Instructions lastInstructions = topN.lastKey();
                if (comparator.compare(lastInstructions, instr) > 0) {
                    collectEntry(instr, actionCreator);
                    if (topN.size() > limit) {
                        topN.remove(lastInstructions);
                    }
                }
            }

        });

    }

    private void collectEntry(final Instructions instructions, final Function<Instructions, Action> actionCreator) {

        if (!topN.containsKey(instructions)) {
            topN.put(instructions, Collections.singletonList(actionCreator));

        } else {

            // only applied in case of duplicate input - we can be inefficient here as this should be an edge case
            // and the overhead for the regular cases should be kept as small as possible
            final List<Function<Instructions, Action>> existingActionCreators = topN.get(instructions);

            final List<Function<Instructions, Action>> allActionCreators = new ArrayList<>(existingActionCreators);
            allActionCreators.add(actionCreator);
            topN.put(instructions, allActionCreators);

        }
    }

    @Override
    public List<Action> createActions() {

        return topN.entrySet().stream()
                .map(entry -> entry.getValue().stream()
                        .map(value -> value.apply(entry.getKey())))
                .flatMap(stream -> stream)
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
