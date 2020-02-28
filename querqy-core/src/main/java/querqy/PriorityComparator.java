package querqy;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class PriorityComparator<T> implements Comparator<T> {

    private final List<Comparator<T>> comparators;

    public PriorityComparator(final List<Comparator<T>> comparators) {
        this.comparators = comparators;
    }


    @Override
    public int compare(final T o1, final T o2) {
        for (final Comparator<T> comparator : comparators) {
            int c = comparator.compare(o1, o2);
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriorityComparator)) return false;
        PriorityComparator<?> that = (PriorityComparator<?>) o;
        return Objects.equals(comparators, that.comparators);
    }

    @Override
    public int hashCode() {
        return Objects.hash(comparators);
    }
}
