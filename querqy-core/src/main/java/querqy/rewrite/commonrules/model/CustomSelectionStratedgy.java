package querqy.rewrite.commonrules.model;

import org.apache.commons.collections4.CollectionUtils;
import querqy.model.Criteria;
import querqy.model.Criterion;
import querqy.model.SelectionCriteria;
import querqy.model.SortCriteria;

import java.util.List;
import java.util.stream.Collectors;

public class CustomSelectionStratedgy implements SelectionStratedgy {

  /**
   * for endeca the selection involves the top first action after sort and filters
   */
  @Override
  public List<Action> selectActions(List<Action> actions, Criterion criterion) {
    if (CollectionUtils.isEmpty(actions) || CollectionUtils.isEmpty(criterion)) {
      return actions;
    }

    List<Action> validActions = actions.parallelStream()
        .filter(action -> isValidAction(criterion, action))
        .collect(Collectors.toList());

    Criteria selectionCriteria = null;
    Criteria sortCriteria = null;

    for (Criteria criteria : criterion) {
      if (criteria instanceof SelectionCriteria) {
        selectionCriteria = criteria;
      } else if (criteria instanceof SortCriteria) {
        sortCriteria = criteria;
      }
    }

    List<Action> sortedAction = validActions;
    if (sortCriteria != null) {
      sortedAction = sortCriteria.apply(validActions);
    }
    if (selectionCriteria != null) {
      return selectionCriteria.apply(sortedAction);
    }
    return sortedAction;
//
//    List<Action> sortedActions = criterion.parallelStream()
//        .filter(criteria -> criteria instanceof SortCriteria)
//        .map(criteria -> criteria.apply(validActions))
//        .collect(Collectors.toList()).parallelStream()
//        .flatMap(List::stream)
//        .collect(Collectors.toList());

  }

  private static boolean isValidAction(Criterion criterion, Action action) {
    return criterion.parallelStream().filter(criteria -> criteria.isValid(action)).count() > 0;
  }
}
