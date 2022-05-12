package querqy.lucene.rewrite;

import org.apache.lucene.index.IndexReader;

import java.io.IOException;

public class SingleFieldBoost implements FieldBoost {

    private final String field;
    private final FieldBoost delegate;

    public SingleFieldBoost(final String field, final FieldBoost delegate) {
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
}
