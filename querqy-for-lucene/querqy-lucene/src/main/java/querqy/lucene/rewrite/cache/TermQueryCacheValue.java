/**
 * 
 */
package querqy.lucene.rewrite.cache;

import querqy.lucene.rewrite.LuceneQueryFactory;
import querqy.lucene.rewrite.LuceneQueryFactoryAndPRMSQuery;
import querqy.lucene.rewrite.prms.PRMSQuery;

/**
 * @author rene
 *
 */
public class TermQueryCacheValue extends LuceneQueryFactoryAndPRMSQuery {
    
    
    public TermQueryCacheValue(LuceneQueryFactoryAndPRMSQuery queryFactoryAndPRMSQuery) {
        this(queryFactoryAndPRMSQuery.queryFactory, queryFactoryAndPRMSQuery.prmsQuery);
    }
    
    public TermQueryCacheValue(LuceneQueryFactory<?> queryFactory, PRMSQuery prmsQuery) {
        super(queryFactory, prmsQuery);
    }
    
    public boolean hasQuery() {
        return queryFactory != null;
    }
    

}
