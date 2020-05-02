package querqy.v2.seqhandler.state;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

// TODO: should be rather named StateContainer
public class SeqState<T> {

    private static final SeqState emptySeqState = new SeqState();
    public static SeqState empty() {
        return emptySeqState;
    }

    private final T value;

    public SeqState() {
        this.value = null;
    }

    public SeqState(T value) {
        this.value = value;
    }

    @Deprecated
    public T getValue() {
        return value;
    }

    public boolean isPresent() {
        return value != null;
    }

    public void ifPresent(Consumer<T> action) {
        if (value != null) {
            action.accept(value);
        }
    }

    public T applyIfPresentOrElseGet(UnaryOperator<T> action, Supplier<T> emptyAction) {
        return value != null ? action.apply(value) : emptyAction.get();
    }

}
