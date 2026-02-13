package querqy.regex;

import java.util.Objects;

class ActiveState<T> {
    final NFAState<T> state; final CaptureEvents captures;

    ActiveState(final NFAState<T> state, final CaptureEvents captures) {
        this.state = state;
        this.captures = captures;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ActiveState other)) return false;
        return state == other.state && captures.equals(other.captures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, captures);
    }


}
