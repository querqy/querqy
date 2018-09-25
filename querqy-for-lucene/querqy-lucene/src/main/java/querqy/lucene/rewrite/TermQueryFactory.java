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
   
    public TermQueryFactory(final Term term) {
       this.term = term;
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
    public TermQuery createQuery(final FieldBoost boost, final float dmqTieBreakerMultiplier,
                                 final TermQueryBuilder termQueryBuilder) {

        return termQueryBuilder.createTermQuery(term, boost);

    }



}
