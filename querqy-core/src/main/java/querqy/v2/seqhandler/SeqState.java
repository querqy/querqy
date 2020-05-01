package querqy.v2.seqhandler;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class SeqState<T> {

    private static final SeqState emptySeqState = new SeqState();
    public static SeqState empty() {
        return emptySeqState;
    }

    private T value = null;

    public SeqState() {}

    public SeqState(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public boolean hasValue() {
        return value != null;
    }

    public void ifPresent(Consumer<T> action) {
        if (value != null) {
            action.accept(value);
        }
    }

    // TODO: proper naming?
    public T applyIfPresentOrElseGet(UnaryOperator<T> action, Supplier<T> emptyAction) {
        return value != null ? action.apply(value) : emptyAction.get();
    }

}
