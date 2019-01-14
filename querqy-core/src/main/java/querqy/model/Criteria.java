package querqy.model;

import querqy.rewrite.commonrules.model.Action;

import java.util.List;

public interface Criteria {

  boolean isValid(Action action);

  List<Action> apply(List<Action> actions);
}
