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
public class DisjunctionMaxQueryFactory extends AbstractLuceneQueryFactory<DisjunctionMaxQuery> {

   protected final LinkedList<LuceneQueryFactory<?>> disjuncts;
   
   public DisjunctionMaxQueryFactory() {
       this(null);
   }

   public DisjunctionMaxQueryFactory(Float boost) {
       super(boost);
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
   public DisjunctionMaxQuery createQuery(Float boostFactor, float dmqTieBreakerMultiplier, DocumentFrequencyCorrection dfc, boolean isBelowDMQ) throws IOException {
       float bf = getBoostFactor(boostFactor);
       if ((!isBelowDMQ) && (dfc != null)) {
           dfc.newClause();
       }
       DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(dmqTieBreakerMultiplier);
       dmq.setBoost(bf);

       for (LuceneQueryFactory<?> disjunct : disjuncts) {
           dmq.add(disjunct.createQuery(null, dmqTieBreakerMultiplier, dfc, true));
       }
       return dmq;
    }

}
