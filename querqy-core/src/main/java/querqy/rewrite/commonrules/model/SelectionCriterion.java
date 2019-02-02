package querqy.rewrite.commonrules.model;

import java.util.List;

public class SelectionCriterion implements Criterion {

    int size;

    public SelectionCriterion(int size) {
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
        return "SelectionCriterion{" +
                "size=" + size +
                '}';
    }
}
