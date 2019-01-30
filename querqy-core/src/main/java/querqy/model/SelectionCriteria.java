package querqy.model;

import querqy.rewrite.commonrules.model.Action;

import java.util.List;

public class SelectionCriteria implements Criteria {

    int size;

    public SelectionCriteria(int size) {
        this.size = size;
    }

    @Override
    public boolean isValid(Action action) {
        return true;
    }

    @Override
    public List<Action> apply(List<Action> actions) {
        if (actions.size() > size) {
            return actions.subList(0, size);
        } else {
            return actions;
        }
    }

    @Override
    public String toString() {
        return "SelectionCriteria{" +
                "size=" + size +
                '}';
    }
}
