package querqy.rewrite.commonrules.select;

import querqy.rewrite.commonrules.model.Action;
import querqy.rewrite.commonrules.model.Instructions;

import java.util.List;
import java.util.function.Function;

public interface TopRewritingActionCollector {

    void offer(List<Instructions> instructions, Function<Instructions, Action> actionCreator);

    List<Action> createActions();

    int getLimit();

    List<? extends FilterCriterion> getFilters();
}
