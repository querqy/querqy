/**
 * 
 */
package querqy.lucene.rewrite.cache;

import querqy.lucene.rewrite.LuceneQueryFactory;

/**
 * @author rene
 *
 */
public class TermQueryCacheValue {
    
    public final LuceneQueryFactory<?> queryFactory;
    
    public TermQueryCacheValue(LuceneQueryFactory<?> queryFactory) {
        this.queryFactory = queryFactory;
    }
    
    public boolean hasQuery() {
        return queryFactory != null;
    }
    

}
