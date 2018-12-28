/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;

import org.apache.lucene.search.Query;

import querqy.lucene.rewrite.prms.PRMSQuery;

/**
 * A LuceneQueryFactory that wraps a TermQueryFactory or the factory of a more complex
 * query that results from Lucene analysis.
 * 
 * @author rene
 *
 */
public class TermSubQueryFactory implements LuceneQueryFactory<Query> {
    
    final LuceneQueryFactory<?> root;
    final FieldBoost boost;
    public final PRMSQuery prmsQuery;
    
    public TermSubQueryFactory(final LuceneQueryFactoryAndPRMSQuery rootAndPrmsQuery, final FieldBoost boost) {
        this(rootAndPrmsQuery.queryFactory, rootAndPrmsQuery.prmsQuery, boost);
    }
    
    public TermSubQueryFactory(final LuceneQueryFactory<?> root, final PRMSQuery prmsQuery, final FieldBoost boost) {
        this.root = root;
        this.boost = boost;
        this.prmsQuery = prmsQuery;
    }

    @Override
    public void prepareDocumentFrequencyCorrection(final DocumentFrequencyCorrection dfc, final boolean isBelowDMQ) {
        root.prepareDocumentFrequencyCorrection(dfc, isBelowDMQ);
    }

    @Override
    public Query createQuery(final FieldBoost boost, final float dmqTieBreakerMultiplier,
                             final TermQueryBuilder termQueryBuilder) {
        
        return root.createQuery(
                this.boost, 
                dmqTieBreakerMultiplier,         
                termQueryBuilder);
    }
    
    public boolean isNeverMatchQuery() {
        return root instanceof NeverMatchQueryFactory;
    }
}
