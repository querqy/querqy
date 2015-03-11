/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.lucene.search.DisjunctionMaxQuery;

/**
 * @author rene
 *
 */
public class DisjunctionMaxQueryFactory implements LuceneQueryFactory<DisjunctionMaxQuery> {

   protected final float boost;
   protected final LinkedList<LuceneQueryFactory<?>> disjuncts;
   protected final float tieBreakerMultiplier;

   public DisjunctionMaxQueryFactory(float boost, float tieBreakerMultiplier) {
      this.boost = boost;
      this.tieBreakerMultiplier = tieBreakerMultiplier;
      disjuncts = new LinkedList<>();
   }

   public void add(LuceneQueryFactory<?> disjunct) {
      disjuncts.add(disjunct);
   }

   public int getNumberOfDisjuncts() {
      return disjuncts.size();
   }

   public LuceneQueryFactory<?> getFirstDisjunct() {
      return disjuncts.getFirst();
   }

   @Override
   public DisjunctionMaxQuery createQuery(DocumentFrequencyCorrection dfc, boolean isBelowDMQ) throws IOException {
      if (!isBelowDMQ) {
         dfc.newClause();
      }
      DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(tieBreakerMultiplier);
      dmq.setBoost(boost);

      for (LuceneQueryFactory<?> disjunct : disjuncts) {
         dmq.add(disjunct.createQuery(dfc, true));
      }
      return dmq;
   }

}
