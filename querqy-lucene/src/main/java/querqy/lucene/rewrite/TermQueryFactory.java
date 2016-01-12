/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

/**
 * @author rene
 *
 */
public class TermQueryFactory implements LuceneQueryFactory<TermQuery> {

    protected final Term term;
   
    public TermQueryFactory(Term term) {
       this.term = term;
   }

    @Override
    public void prepareDocumentFrequencyCorrection(DocumentFrequencyCorrection dfc, boolean isBelowDMQ) {

        if (!isBelowDMQ) {
            // a TQ might end up directly under a BQ as an optimisation
            // make sure, we start a new clause in df correction
            dfc.newClause();
        }

        dfc.prepareTerm(term);

    }

    @Override
    public TermQuery createQuery(FieldBoost boost, float dmqTieBreakerMultiplier, DocumentFrequencyCorrection dfc)
            throws IOException {

        return new DependentTermQuery(term, dfc, boost);

    }



}
