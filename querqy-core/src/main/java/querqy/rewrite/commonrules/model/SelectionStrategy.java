package querqy.rewrite.commonrules.model;

import java.util.List;

public interface SelectionStrategy {
    List<Action> selectActions(List<Action> actions, Criteria criteria);
}