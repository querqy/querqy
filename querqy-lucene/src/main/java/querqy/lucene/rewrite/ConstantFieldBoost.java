package querqy.lucene.rewrite;

import org.apache.lucene.index.IndexReader;

import java.io.IOException;

/**
 * Created by rene on 11/01/2016.
 */
public class ConstantFieldBoost implements FieldBoost {

    public static final ConstantFieldBoost NORM_BOOST = new ConstantFieldBoost(1f);

    final float boost;

    public ConstantFieldBoost(float boost) { this.boost = boost; }

    @Override
    public float getBoost(String fieldname, IndexReader indexReader)
            throws IOException {
        return boost;
    }

    @Override
    public void registerTermSubQuery(String fieldname,
                                     TermSubQueryFactory termSubQueryFactory,
                                     querqy.model.Term sourceTerm) {
    }

    @Override
    public String toString(String fieldname) {
        return "ConstantFieldBoost(" + fieldname + "^" + boost + ")";
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConstantFieldBoost that = (ConstantFieldBoost) o;

        return Float.compare(that.boost, boost) == 0;

    }

    @Override
    public int hashCode() {
        return (boost != +0.0f ? Float.floatToIntBits(boost) : 0);
    }
}
