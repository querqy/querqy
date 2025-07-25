package querqy.lucene.rewrite;

import querqy.lucene.rewrite.prms.PRMSQuery;

public class LuceneQueryFactoryAndPRMSQuery {
    public final LuceneQueryFactory<?> queryFactory;
    public final PRMSQuery prmsQuery;

    public LuceneQueryFactoryAndPRMSQuery(LuceneQueryFactory<?> queryFactory,
            PRMSQuery prmsQuery) {
        this.queryFactory = queryFactory;
        this.prmsQuery = prmsQuery;
    }
    
}