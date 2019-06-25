package querqy.rewrite.commonrules.model;

import java.util.List;
import java.util.function.Function;

public interface TopRewritingActionCollector {

    void offer(List<Instructions> instructions, Function<Instructions, Action> actionCreator);

    List<Action> createActions();

    int getLimit();

    List<? extends FilterCriterion> getFilters();
}
