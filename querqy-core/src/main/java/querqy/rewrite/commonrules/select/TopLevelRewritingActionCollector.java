package querqy.rewrite.commonrules.select;

import querqy.rewrite.commonrules.model.Action;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.Limit;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <p>A {@link TopLevelRewritingActionCollector} that interprets the number of rules that is applied when
 * {@link Limit#isUseLevels()} is set to be true. That means that rules that have the same value for the chosen
 * sort property count only once towards the number of rules to be applied. For example, if we have six rules and the
 * order of their sort properties is 2, 2, 2, 8, 8, 9, only the first three rules (2, 2, 2) will be applied if the limit
 * is set to 1. This sort order will be applied using only the first comparator in the list of comparators that is
 * passed to the constructor. Any further comparators passed to the constructor will be used as a secondary
 * (or ternary etc.) sort order to sort the rules that fall within the limit. If only one comparator is passed to
 * the constructor, the rule definition order will serve as a secondary criterion.</p>
 *
 * @author René Kriegler, @renekrie
 *
 */
public class TopLevelRewritingActionCollector extends TopRewritingActionCollector {

    private static List<Comparator<Instructions>> DEFAULT_SECONDARY_COMPARATORS = Collections.singletonList(
            Comparator.comparingInt(Instructions::getOrd));

    private final TreeMap<Instructions, TreeSet<Element>> topN;
    private final int limit;
    private List<? extends FilterCriterion> filters;
    private final Comparator<Instructions> primaryComparator;
    private final List<Comparator<Instructions>> secondaryComparators;

    // TODO: check if redundant query input is handled properly
    public TopLevelRewritingActionCollector(final List<Comparator<Instructions>> comparators, final int limit,
                                            final List<? extends FilterCriterion> filters) {

        final int numComparators = comparators.size();
        if (numComparators == 0) {
            throw new IllegalArgumentException("comparators expected");
        }
        if (limit < 0) {
            throw new IllegalArgumentException("limit > -1 expected");
        }

        primaryComparator = comparators.get(0);
        secondaryComparators = numComparators == 1
                ? DEFAULT_SECONDARY_COMPARATORS
                : comparators.subList(1, numComparators);

        topN = new TreeMap<>(primaryComparator);
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

            final TreeSet<Element> elementsSoFar = topN.get(instr);
            if (elementsSoFar != null) {

                elementsSoFar.add(new Element(instr, actionCreator));

            } else if (topN.size() < limit) {

                final TreeSet<Element> newElements = new TreeSet<>();
                newElements.add(new Element(instr, actionCreator));
                topN.put(instr, newElements);

            } else {
                final Instructions lastInstructions = topN.lastKey();

                if (primaryComparator.compare(lastInstructions, instr) > 0) {

                    final TreeSet<Element> newElements = new TreeSet<>();
                    newElements.add(new Element(instr, actionCreator));
                    topN.put(instr, newElements);

                    if (topN.size() > limit) {
                        topN.remove(lastInstructions);
                    }
                }
            }

        });

    }

    @Override
    public List<Action> createActions() {

        return topN.values().stream()
                .flatMap(Collection::stream)
                .map(Element::get)
                .collect(Collectors.toList());

    }

    public int getLimit() {
        return limit;
    }

    public List<? extends FilterCriterion> getFilters() {
        return filters;
    }


    class Element implements Comparable<Element>, Supplier<Action> {

        final Instructions instructions;
        final Function<Instructions, Action> func;


        Element(final Instructions instructions, final Function<Instructions, Action> func) {
            this.instructions = instructions;
            this.func = func;
        }

        @Override
        public int compareTo(final Element other) {
            for (final Comparator<Instructions> comparator : secondaryComparators) {
                int c = comparator.compare(this.instructions, other.instructions);
                if (c != 0) {
                    return c;
                }
            }
            return 0;
        }

        @Override
        public Action get() {
            return func.apply(instructions);
        }
    }
}
