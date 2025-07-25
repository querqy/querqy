/**
 * 
 */
package querqy.lucene.rewrite;

import org.apache.lucene.search.Query;

import querqy.lucene.rewrite.prms.PRMSQuery;
import querqy.model.Term;

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
    private final Term sourceTerm;
    private final String fieldname;
    
    public TermSubQueryFactory(final LuceneQueryFactoryAndPRMSQuery rootAndPrmsQuery, final FieldBoost boost,
                               final Term sourceTerm, final String fieldname) {
        this(rootAndPrmsQuery.queryFactory, rootAndPrmsQuery.prmsQuery, boost, sourceTerm, fieldname);
    }
    
    public TermSubQueryFactory(final LuceneQueryFactory<?> root, final PRMSQuery prmsQuery, final FieldBoost boost,
                               final Term sourceTerm, final String fieldname) {
        this.root = root;
        this.boost = boost;
        this.prmsQuery = prmsQuery;
        this.sourceTerm = sourceTerm;
        this.fieldname = fieldname;
    }

    @Override
    public void prepareDocumentFrequencyCorrection(final DocumentFrequencyCorrection dfc, final boolean isBelowDMQ) {
        root.prepareDocumentFrequencyCorrection(dfc, isBelowDMQ);
    }

    @Override
    public Query createQuery(final FieldBoost boost, final TermQueryBuilder termQueryBuilder) {
        
        return root.createQuery(this.boost, termQueryBuilder);
    }
    
    public boolean isNeverMatchQuery() {
        return root instanceof NeverMatchQueryFactory;
    }

    @Override
    public <R> R accept(final LuceneQueryFactoryVisitor<R> visitor) {
        return visitor.visit(this);
    }

    public String getFieldname() {
        return fieldname;
    }

    public Term getSourceTerm() {
        return sourceTerm;
    }
}
