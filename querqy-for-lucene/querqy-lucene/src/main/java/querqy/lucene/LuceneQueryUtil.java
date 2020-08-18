package querqy.lucene;

import org.apache.lucene.queries.function.FunctionQuery;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.valuesource.QueryValueSource;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import querqy.LowerCaseCharSequence;

/**
 * Created by rene on 09/04/2016.
 */
public interface LuceneQueryUtil {

    /**
     * Wrap a query with a {@link BoostQuery} if the boost factor doesn't equal 1.
     *
     * @param query The query to boost
     * @param boostFactor The boost factor
     * @return A BoostQuery if boostFactor != 1 or the original query in all other cases.
     */
    static Query boost(final Query query, final float boostFactor) {
        return boostFactor == 1f ? query : new BoostQuery(query, boostFactor);
    }

    static ValueSource queryToValueSource(final Query query) {
        return (query instanceof FunctionQuery)
                ? ((FunctionQuery)query).getValueSource()
                : new QueryValueSource(query, 1.0f);
    }

    static DoubleValuesSource queryToDoubleValueSource(final Query query) {
        return (query instanceof FunctionScoreQuery)
                ? ((FunctionScoreQuery)query).getSource()
                : DoubleValuesSource.fromQuery(query);
    }

    static org.apache.lucene.index.Term toLuceneTerm(final String fieldname, final CharSequence value,
                                                            final boolean lowerCaseInput) {

        final BytesRef bytesRef = (value instanceof LowerCaseCharSequence || !lowerCaseInput)
                ? new BytesRef(value)
                : new BytesRef(new LowerCaseCharSequence(value));

        return new org.apache.lucene.index.Term(fieldname, bytesRef);

    }
}
