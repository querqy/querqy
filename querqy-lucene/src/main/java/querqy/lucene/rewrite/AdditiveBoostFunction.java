package querqy.lucene.rewrite;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.docvalues.FloatDocValues;

import java.io.IOException;
import java.util.Map;

public class AdditiveBoostFunction extends ValueSource {

    private final float boostValue;
    private final ValueSource scoringFunction;
    private final boolean isNegBoost;

    public AdditiveBoostFunction(final ValueSource scoringFunction, final float boost) {
        this.scoringFunction = scoringFunction;
        isNegBoost = boost < 0;
        boostValue = Math.abs(boost);
    }


    @Override
    public FunctionValues getValues(final Map context, final LeafReaderContext readerContext) throws IOException {

        final FunctionValues scoringFunctionValues = scoringFunction.getValues(context, readerContext);

        return new FloatDocValues(this) {

            @Override
            public float floatVal(final int doc) throws IOException {

                if (!scoringFunctionValues.exists(doc)) {
                    return isNegBoost ? boostValue : 0f;
                }

                final float score = scoringFunctionValues.floatVal(doc);
                return isNegBoost
                        ? boostValue * (1f - (1f - 1f/(score + 1f)))
                        : boostValue * (1f - (     1f/(score + 1f)));

            }

            @Override
            public boolean exists(final int doc) {
                return true;
            }

            @Override
            public String toString(int doc) throws IOException {
                return "AdditiveBoostFunction" + '(' + (isNegBoost ? -boostValue : boostValue)  + ',' +
                        scoringFunctionValues.toString(doc) + ')';
            }
        };
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AdditiveBoostFunction)) {
            return false;
        }
        final AdditiveBoostFunction that = (AdditiveBoostFunction) o;
        if (this.isNegBoost != that.isNegBoost || this.boostValue != that.boostValue) {
            return false;
        }
        return this.scoringFunction.equals(that.scoringFunction);
    }

    @Override
    public int hashCode() {
        return Float.hashCode(boostValue) + scoringFunction.hashCode();
    }

    @Override
    public String description() {
        return "AdditiveBoostFunction(" + (isNegBoost ? -boostValue : boostValue)  + ',' +
                this.scoringFunction.description() + ')';
    }
}
