package querqy.rewrite.commonrules.model;

import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

public class CustomSelectionStrategy implements SelectionStrategy {

    /**
     * for endeca the selection involves the top first action after sort and filters
     */
    @Override
    public List<Action> selectActions(List<Action> actions, Criteria criteria) {
        if (CollectionUtils.isEmpty(actions) || CollectionUtils.isEmpty(criteria)) {
            return actions;
        }

        List<Action> validActions = actions.parallelStream()
                .filter(action -> isValidAction(criteria, action))
                .collect(Collectors.toList());

        Criterion selectionCriterion = null;
        Criterion sortCriterion = null;

        for (Criterion criterion : criteria) {
            if (criterion instanceof SelectionCriterion) {
                selectionCriterion = criterion;
            } else if (criterion instanceof SortCriterion) {
                sortCriterion = criterion;
            }
        }

        List<Action> sortedAction = validActions;
        if (sortCriterion != null) {
            sortedAction = sortCriterion.apply(validActions);
        }
        if (selectionCriterion != null) {
            return selectionCriterion.apply(sortedAction);
        }
        return sortedAction;
//
//    List<Action> sortedActions = criterion.parallelStream()
//        .filter(criteria -> criteria instanceof SortCriterion)
//        .map(criteria -> criteria.apply(validActions))
//        .collect(Collectors.toList()).parallelStream()
//        .flatMap(List::stream)
//        .collect(Collectors.toList());

    }

    private static boolean isValidAction(final Criteria criteria, final Action action) {
        return criteria.parallelStream().filter(criterion -> criterion.isValid(action)).count() > 0;
    }
}
