/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class NeverMatchQueryFactory implements LuceneQueryFactory<Query> {
    
    public static final NeverMatchQueryFactory FACTORY = new NeverMatchQueryFactory();

    @Override
    public Query createQuery(FieldBoost boostFactor, float dmqTieBreakerMultiplier,
                             DocumentFrequencyAndTermContextProvider dftcp, boolean isBelowDMQ)
            throws IOException {
        return new BooleanQuery();
    }


}
