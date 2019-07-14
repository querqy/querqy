package querqy.lucene;

import org.apache.lucene.queries.function.FunctionQuery;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.valuesource.QueryValueSource;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.Query;

/**
 * Created by rene on 09/04/2016.
 */
public class LuceneQueryUtil {

    /**
     * Wrap a query with a {@link BoostQuery} if the boost factor doesn't equal 1.
     *
     * @param query The query to boost
     * @param boostFactor The boost factor
     * @return A BoostQuery if boostFactor != 1 or the original query in all other cases.
     */
    public static Query boost(final Query query, final float boostFactor) {
        return boostFactor == 1f ? query : new BoostQuery(query, boostFactor);
    }

    public static ValueSource queryToValueSource(final Query query) {
        return (query instanceof FunctionQuery)
                ? ((FunctionQuery)query).getValueSource()
                : new QueryValueSource(query, 1.0f);
    }

    public static DoubleValuesSource queryToDoubleValueSource(final Query query) {
        return (query instanceof FunctionScoreQuery)
                ? ((FunctionScoreQuery)query).getSource()
                : DoubleValuesSource.fromQuery(query);
    }
}
