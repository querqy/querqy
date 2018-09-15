package querqy.lucene;

import org.apache.lucene.search.BoostQuery;
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
}
