package querqy.v2.seqhandler;

@FunctionalInterface
public interface StateHandler<T> {

    SeqState<T> handleSequence(final QueryStateView<T> queryStateView);

}
