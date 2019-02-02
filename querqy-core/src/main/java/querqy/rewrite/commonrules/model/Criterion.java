package querqy.rewrite.commonrules.model;

import java.util.List;

public interface Criterion {

    boolean isValid(Action action);

    List<Action> apply(List<Action> actions);
}