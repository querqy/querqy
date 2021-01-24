package querqy.model;

import java.util.Objects;

public class BoostedTerm extends Term {

    private final float boost;

    public BoostedTerm(final DisjunctionMaxQuery parentQuery, final String field, final CharSequence value,
            float boost) {
        super(parentQuery, field, value, true);
        
        this.boost = boost;
    }

    public BoostedTerm(final DisjunctionMaxQuery parentQuery, final CharSequence value, float boost) {
        this(parentQuery, null, value, boost);
    }

    public float getBoost() {
        return boost;
    }

    @Override
    public <T> T accept(final NodeVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return ((field == null) ? "*" : field) + "(" + boost + "):" + getValue();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), boost);
    }
}
