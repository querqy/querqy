package querqy.lucene.rewrite;

import org.apache.lucene.index.IndexReader;

import java.io.IOException;
import java.util.Objects;

/**
 * This {@link FieldBoost} implementation passes through the boost value for a single given field from a delegate
 * FieldBoost and return 0 for all other fields.
 */
public class SingleFieldBoost implements FieldBoost {

    private final String field;
    private final FieldBoost delegate;

    /**
     * @param field The field to pass through the boost from the delegate
     * @param delegate The delegate FieldBoost
     */
    public SingleFieldBoost(final String field, final FieldBoost delegate) {
        if (field == null) {
            throw new IllegalArgumentException("Field name must not be null");
        }
        if (delegate == null) {
            throw new IllegalArgumentException("FieldBoost delegate must not be null");
        }
        this.field = field;
        this.delegate = delegate;
    }


    @Override
    public float getBoost(final String fieldname, final IndexReader indexReader) throws IOException {
        return field.equals(fieldname) ? delegate.getBoost(fieldname, indexReader) : 0f;
    }

    @Override
    public void registerTermSubQuery(final TermSubQueryFactory termSubQueryFactory) {
        delegate.registerTermSubQuery(termSubQueryFactory);
    }

    @Override
    public String toString(final String fieldname) {
        return "^SingleFieldBoost(" + (field.equals(fieldname) ? delegate.toString(fieldname) : "0.0") + ")";
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final SingleFieldBoost that = (SingleFieldBoost) obj;
        return field.equals(that.field) && delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, delegate);
    }
}
