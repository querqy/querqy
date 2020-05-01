package querqy.v2;

import querqy.v2.model.QueryStateView;
import querqy.v2.model.SeqState;

@FunctionalInterface
public interface StateHandler<T> {

    SeqState<T> handleSequence(final QueryStateView<T> queryStateView);

}
