package querqy.rewrite.commonrules.model;

import java.util.List;

public class DefaultSelectionStrategy implements SelectionStrategy {
    @Override
    public List<Action> selectActions(final List<Action> actions, final Criteria criteria) {
        return actions;
    }
}
