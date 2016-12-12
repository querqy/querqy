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
    public void prepareDocumentFrequencyCorrection(DocumentFrequencyAndTermContextProvider dftcp, boolean isBelowDMQ) {

        if (dftcp != null) {

            if (!isBelowDMQ) {
                // a TQ might end up directly under a BQ as an optimisation
                // make sure, we start a new clause in df correction
                dftcp.newClause();
            }

            dftcp.prepareTerm(term);

        }

    }

    @Override
    public TermQuery createQuery(FieldBoost boost, float dmqTieBreakerMultiplier, DocumentFrequencyAndTermContextProvider dftcp)
            throws IOException {

        return dftcp != null
                ? new DependentTermQuery(term, dftcp, boost)
                : new TermBoostQuery(term, boost)
                ;

    }



}
