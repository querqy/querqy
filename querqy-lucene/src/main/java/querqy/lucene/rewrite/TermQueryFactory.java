/**
 * 
 */
package querqy.lucene.rewrite;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

/**
 * @author rene
 *
 */
public class TermQueryFactory implements LuceneQueryFactory<TermQuery> {

    protected final Term term;
    protected final querqy.model.Term sourceTerm;
   
    public TermQueryFactory(final Term term, final querqy.model.Term sourceTerm) {
        this.term = term;
        this.sourceTerm = sourceTerm;
    }

    @Override
    public void prepareDocumentFrequencyCorrection(final DocumentFrequencyCorrection dfc, final boolean isBelowDMQ) {

        if (!isBelowDMQ) {
            // a TQ might end up directly under a BQ as an optimisation
            // make sure, we start a new clause in df correction
            dfc.newClause();
        }

        dfc.prepareTerm(term);

    }

    @Override
    public TermQuery createQuery(final FieldBoost boost, final TermQueryBuilder termQueryBuilder) {

        return termQueryBuilder.createTermQuery(term, boost);

    }

    @Override
    public <R> R accept(final LuceneQueryFactoryVisitor<R> visitor) {
        return visitor.visit(this);
    }

    public String getFieldname() {
        return term.field();
    }
}
