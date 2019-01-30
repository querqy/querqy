package querqy.rewrite.commonrules.model;

import querqy.model.Criterion;

import java.util.List;

public interface SelectionStratedgy {
    public List<Action> selectActions(List<Action> actions, Criterion criterion);
}