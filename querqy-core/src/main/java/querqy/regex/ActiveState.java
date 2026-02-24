package querqy.regex;

import querqy.regex.NFAState.SuffixTransition;

import java.util.Objects;
import java.util.Set;

class ActiveState<T> {

    final NFAState<T> state; final CaptureEvents captures;

    ActiveState(final NFAState<T> state, final CaptureEvents captures) {
        this.state = state;
        this.captures = captures;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final ActiveState<?> that = (ActiveState<?>) o;
        return Objects.equals(state, that.state) && Objects.equals(captures, that.captures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, captures);
    }

    public Set<RegexEntry<T>> getAccepting() {
        return state.accepting;
    }

    public static class SuffixActiveState<T> extends ActiveState<T> {

        final SuffixTransition<T> suffixTransition;


        SuffixActiveState(final NFAState<T> state, final CaptureEvents captures, final SuffixTransition<T> suffixTransition) {
            super(state, captures);
            this.suffixTransition = suffixTransition;
        }

        public Set<RegexEntry<T>> getAccepting() {
            return suffixTransition.accepting();
        }

        @Override
        public boolean equals(final Object o) {
            if (!super.equals(o)) return false;
            final SuffixActiveState<?> that = (SuffixActiveState<?>) o;
            return Objects.equals(suffixTransition, that.suffixTransition);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), suffixTransition);
        }
    }


}
