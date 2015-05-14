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

    @Override
    public Query createQuery(Float boostFactor, float dmqTieBreakerMultiplier, DocumentFrequencyCorrection dfc, boolean isBelowDMQ)
            throws IOException {
        return new BooleanQuery();
    }

   

}
