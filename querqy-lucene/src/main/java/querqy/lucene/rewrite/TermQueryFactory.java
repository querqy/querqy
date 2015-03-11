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
   protected final float boost;

   public TermQueryFactory(Term term, float boost) {
      this.term = term;
      this.boost = boost;
   }

   @Override
   public TermQuery createQuery(DocumentFrequencyCorrection dfc, boolean isBelowDMQ) throws IOException {

      if (!isBelowDMQ) {
         // a TQ might end up directly under a BQ as an optimisation
         // make sure, we start a new clause in df correction
         dfc.newClause();
      }

      DocumentFrequencyCorrectedTermQuery tq = new DocumentFrequencyCorrectedTermQuery(term, dfc);
      tq.setBoost(boost);
      return tq;

   }


}
