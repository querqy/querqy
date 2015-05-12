/**
 * 
 */
package querqy.solr;

import org.apache.solr.search.SolrCache;

import querqy.lucene.rewrite.cache.CacheKey;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.lucene.rewrite.cache.TermQueryCacheValue;

/**
 * @author rene
 *
 */
public class TermQueryCacheAdapter implements TermQueryCache {
    
    final SolrCache<CacheKey, TermQueryCacheValue> delegate;
    
    public TermQueryCacheAdapter(SolrCache<CacheKey, TermQueryCacheValue> delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("Solr cache must not be null");
        }
        this.delegate = delegate;
    }

    @Override
    public TermQueryCacheValue put(CacheKey cacheKey, TermQueryCacheValue value) {
        return delegate.put(cacheKey, value);
    }

    @Override
    public TermQueryCacheValue get(CacheKey cacheKey) {
        return delegate.get(cacheKey);
    }

}
