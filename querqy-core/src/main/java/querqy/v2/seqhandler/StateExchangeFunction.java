package querqy.v2.seqhandler;

import querqy.v2.seqhandler.state.QueryStateView;
import querqy.v2.seqhandler.state.SeqState;

@FunctionalInterface
public interface StateExchangeFunction<T> {

    SeqState<T> exchangeState(final QueryStateView<T> queryStateView);

}
