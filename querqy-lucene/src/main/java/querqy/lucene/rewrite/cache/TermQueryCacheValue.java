/**
 * 
 */
package querqy.lucene.rewrite.cache;

import java.io.IOException;

import org.apache.lucene.search.Query;

import querqy.lucene.rewrite.DocumentFrequencyCorrection;
import querqy.lucene.rewrite.LuceneQueryFactory;

/**
 * @author rene
 *
 */
public class TermQueryCacheValue implements LuceneQueryFactory<Query>{
    
    public final Query query;
    
    public TermQueryCacheValue(Query query) {
        this.query = query;
    }
    
    public boolean hasQuery() {
        return query != null;
    }

    @Override
    public Query createQuery(DocumentFrequencyCorrection dfc, boolean isBelowDMQ)
            throws IOException {
        // FIXME: dfc / boost factors
        return query;
    }

}
