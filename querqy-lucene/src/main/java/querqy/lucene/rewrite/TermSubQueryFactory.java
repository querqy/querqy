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
    
    public TermSubQueryFactory(LuceneQueryFactoryAndPRMSQuery rootAndPrmsQuery, FieldBoost boost) {
        this(rootAndPrmsQuery.queryFactory, rootAndPrmsQuery.prmsQuery, boost);
    }
    
    public TermSubQueryFactory(LuceneQueryFactory<?> root, PRMSQuery prmsQuery, FieldBoost boost) {
        this.root = root;
        this.boost = boost;
        this.prmsQuery = prmsQuery;
    }

    @Override
    public void prepareDocumentFrequencyCorrection(DocumentFrequencyCorrection dfc, boolean isBelowDMQ) {
        root.prepareDocumentFrequencyCorrection(dfc, isBelowDMQ);
    }

    @Override
    public Query createQuery(FieldBoost boost, float dmqTieBreakerMultiplier, DocumentFrequencyCorrection dfc)
            throws IOException {
        
        return root.createQuery(
                this.boost, 
                dmqTieBreakerMultiplier,         
                dfc);
    }
    
    public boolean isNeverMatchQuery() {
        return root instanceof NeverMatchQueryFactory;
    }
}
