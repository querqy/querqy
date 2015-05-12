/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;

import org.apache.lucene.search.Query;

import querqy.lucene.rewrite.cache.CacheKey;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.lucene.rewrite.cache.TermQueryCacheValue;

/**
 * @author rene
 *
 */
public class CachingQueryFactory<T extends Query> implements LuceneQueryFactory<T> {
    
    final TermQueryCache termQueryCache;
    final LuceneQueryFactory<T> cached;
    final CacheKey cacheKey;
    
    public CachingQueryFactory(TermQueryCache termQueryCache, CacheKey cacheKey, LuceneQueryFactory<T> cached) {
        if (cached == null) {
            throw new IllegalArgumentException("Query to be cached must not be null");
        }
        this.cached = cached;
        this.termQueryCache = termQueryCache;
        this.cacheKey = cacheKey;
    }

    @Override
    public T createQuery(DocumentFrequencyCorrection dfc, boolean isBelowDMQ)
            throws IOException {
        // FIXME: handle params
        T query = cached.createQuery(dfc, isBelowDMQ);
        termQueryCache.put(cacheKey, new TermQueryCacheValue(query));
        return query;
    }

}
