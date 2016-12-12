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
public class SolrTermQueryCacheAdapter implements TermQueryCache {
    
    final SolrCache<CacheKey, TermQueryCacheValue> delegate;
    final boolean ignoreUpdates;
    
    public SolrTermQueryCacheAdapter(boolean ignoreUpdates, SolrCache<CacheKey, TermQueryCacheValue> delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("Solr cache must not be null");
        }
        this.delegate = delegate;
        this.ignoreUpdates = ignoreUpdates;
    }

    @Override
    public void put(CacheKey cacheKey, TermQueryCacheValue value) {
        if (!ignoreUpdates) {
            delegate.put(cacheKey, value);
        }
    }

    @Override
    public TermQueryCacheValue get(CacheKey cacheKey) {
        return delegate.get(cacheKey);
    }

}
