/**
 * 
 */
package querqy.lucene.rewrite.cache;

/**
 * @author rene
 *
 */
public interface TermQueryCache {
    
    TermQueryCacheValue put(CacheKey key, TermQueryCacheValue value);
    
    TermQueryCacheValue get(CacheKey key);

}
