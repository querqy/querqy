package querqy.rewrite.lookup.model;

import querqy.model.Term;

import java.util.List;
import java.util.Objects;

public class Sequence<T> {

    private final T state;
    private final List<Term> terms;

    private Sequence(final T state, final List<Term> terms) {
        this.state = state;
        this.terms = terms;
    }

    public T getState() {
        return state;
    }

    public List<Term> getTerms() {
        return terms;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sequence<?> sequence = (Sequence<?>) o;
        return state.equals(sequence.state) && terms.equals(sequence.terms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, terms);
    }

    public static <T> Sequence<T> of(final T state, final List<Term> terms) {
        return new Sequence<>(state, terms);
    }
}
