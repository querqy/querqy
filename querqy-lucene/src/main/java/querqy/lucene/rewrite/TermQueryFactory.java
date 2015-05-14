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
public class TermQueryFactory extends AbstractLuceneQueryFactory<TermQuery> {

   protected final Term term;
   
   public TermQueryFactory(Term term) {
       this(term, null);
   }

   public TermQueryFactory(Term term, Float boost) {
       super(boost);
       this.term = term;
   }

   @Override
   public TermQuery createQuery(Float boostFactor, float dmqTieBreakerMultiplier, DocumentFrequencyCorrection dfc, boolean isBelowDMQ) throws IOException {
       
       float bf = getBoostFactor(boostFactor);
       
       TermQuery tq = null;
       
       if (dfc == null) {
       
           tq = new TermQuery(term);
       
       } else {

           if (!isBelowDMQ) {
               // a TQ might end up directly under a BQ as an optimisation
               // make sure, we start a new clause in df correction
               dfc.newClause();
           }

           tq = new DocumentFrequencyCorrectedTermQuery(term, dfc);
       }
       
       tq.setBoost(bf);
       
       return tq;

   }


}
