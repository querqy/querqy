package querqy.rewrite.commonrules.model;

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TopRewritingActionCollector {

    private final TreeMap<Instructions, Function<Instructions, Action>> topN;
    private final int limit;
    private List<? extends Criterion> criteria;
    private final Comparator<Instructions> comparator;

    public TopRewritingActionCollector(final Comparator<Instructions> comparator, final int limit,
                                       final List<? extends Criterion> criteria) {
        topN = new TreeMap<>(comparator);
        this.limit = limit;
        this.criteria = criteria;
        this.comparator = comparator;
    }


    public void offer(final List<Instructions> instructions, final Function<Instructions, Action> actionCreator) {

        if (limit == 0) {
            return;
        }

        instructions.stream()
                .filter(instr -> {
                    for (final Criterion criterion : criteria) {
                        if (!criterion.isValid(instr)) {
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

    public List<Action> createActions() {

        return topN.entrySet().stream()
                .map(entry -> entry.getValue().apply(entry.getKey()))
                .collect(Collectors.toList());

    }


}
