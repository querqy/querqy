package querqy.rewrite.commonrules.model;

import java.util.Objects;

/**
 * A limit to the number of rules to be applied.
 *
 * @author René Kriegler, renekrie
 */
public class Limit {

    /**
     * The number of rules to be applied
     */
    private final int count;

    /**
     * Iff true, rules that have the same value for the sort value will only {@link #count} once to the number of rules
     * to be applied.
     */
    private final boolean useLevels;

    public Limit(final boolean useLevels) {
        this.count = -1;
        this.useLevels = useLevels;
    }

    public Limit(final int count) {
        this(count, false);
    }

    public Limit(final int count, final boolean useLevels) {
        this.count = Math.max(-1, count);
        this.useLevels = this.count >= 1 && useLevels;
    }

    public int getCount() {
        return count;
    }

    public boolean isUseLevels() {
        return useLevels;
    }

    public boolean isSet() {
        return count > -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Limit)) return false;
        Limit limit = (Limit) o;
        return count == limit.count &&
                useLevels == limit.useLevels;
    }

    @Override
    public int hashCode() {
        return Objects.hash(count, useLevels);
    }
}
