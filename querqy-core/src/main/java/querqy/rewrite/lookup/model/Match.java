package querqy.rewrite.lookup.model;

import querqy.rewrite.commonrules.model.TermMatches;

import java.util.Objects;

public class Match<T> {

    private final TermMatches termMatches;
    private final T value;

    private Match(final TermMatches termMatches, final T value) {
        this.termMatches = termMatches;
        this.value = value;
    }

    public TermMatches getTermMatches() {
        return termMatches;
    }

    public T getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Match<?> match = (Match<?>) o;
        return Objects.equals(termMatches, match.termMatches) && Objects.equals(value, match.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(termMatches, value);
    }

    @Override
    public String toString() {
        return "Match{" +
                "termMatches=" + termMatches +
                ", value=" + value +
                '}';
    }

    public static <T> Match<T> of(final TermMatches termMatches, final T value) {
        return new Match<>(termMatches, value);
    }
}
