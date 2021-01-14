package querqy.lucene.rewrite;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;

import querqy.model.Term;

public class BoostedDelegatingFieldBoost implements FieldBoost {

    private final FieldBoost delegate;
    private final float boost;

    public BoostedDelegatingFieldBoost(final FieldBoost delegate, final float boost) {
        this.delegate = delegate;
        this.boost = boost;
    }

    @Override
    public float getBoost(final String fieldname, final IndexReader indexReader) throws IOException {
        final float computedBoost = delegate.getBoost(fieldname, indexReader);
        return computedBoost * boost;
    }

    @Override
    public void registerTermSubQuery(final String fieldname, final TermSubQueryFactory termSubQueryFactory, final Term sourceTerm) {
        delegate.registerTermSubQuery(fieldname, termSubQueryFactory, sourceTerm);
    }

    @Override
    public String toString(final String fieldname) {
        return delegate.toString(fieldname);
    }


}
