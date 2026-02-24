package querqy.regex;

import java.util.Set;

public class NFAFragment<T> {

    final NFAState<T> start;
    final Set<NFAState<T>> accepts;

    public NFAFragment(final NFAState<T> start, final Set<NFAState<T>> accepts) {
        this.start = start;
        this.accepts = accepts;
    }
    public static <T> NFAFragment<T> single(final NFAState<T> start, final NFAState<T> accept) {
        return new NFAFragment<T>(start, Set.of(accept));
    }

    public static <T> NFAFragment<T> empty() {
        final NFAState<T> state = new NFAState<T>();
        return single(state, state);
    }
}

