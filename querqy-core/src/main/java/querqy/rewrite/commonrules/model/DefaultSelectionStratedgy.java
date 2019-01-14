package querqy.rewrite.commonrules.model;

import querqy.model.Criterion;

import java.util.List;

public class DefaultSelectionStratedgy implements SelectionStratedgy {


  @Override
  public List<Action> selectActions(List<Action> actions,
                                    Criterion criterion) {
    return actions;
  }
}
