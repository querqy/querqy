package querqy.trie.model;

import java.util.Objects;

public class ExactMatch<T> {

    public final int lookupStart;
    public final int lookupExclusiveEnd;
    public final int termSize;

    public final T value;

    public ExactMatch(final int lookupStart, final int lookupExclusiveEnd, final T value) {
        this.lookupStart = lookupStart;
        this.lookupExclusiveEnd = lookupExclusiveEnd;
        this.termSize = lookupExclusiveEnd - lookupStart;
        this.value = value;
    }

    @Override
    public String toString() {
        return "ExactMatch{" +
                "lookupStart=" + lookupStart +
                ", lookupExclusiveEnd=" + lookupExclusiveEnd +
                ", value=" + value +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExactMatch<?> that = (ExactMatch<?>) o;
        return lookupStart == that.lookupStart &&
                lookupExclusiveEnd == that.lookupExclusiveEnd &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lookupStart, lookupExclusiveEnd, value);
    }
}
