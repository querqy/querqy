/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.Query;


/**
 * @author rene
 *
 */
public class DisjunctionMaxQueryFactory implements LuceneQueryFactory<DisjunctionMaxQuery> {

    protected final LinkedList<LuceneQueryFactory<?>> disjuncts;
   
    public DisjunctionMaxQueryFactory() {
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
    public void prepareDocumentFrequencyCorrection(final DocumentFrequencyCorrection dfc, final boolean isBelowDMQ) {

        if (!isBelowDMQ) {
            dfc.newClause();
        }

        for (final LuceneQueryFactory<?> disjunct : disjuncts) {
            disjunct.prepareDocumentFrequencyCorrection(dfc, true);
        }

    }

    @Override
    public DisjunctionMaxQuery createQuery(final FieldBoost boost, final float dmqTieBreakerMultiplier,
                                           final TermQueryBuilder termQueryBuilder) {

        final List<Query> disjunctList = new LinkedList<>();

        for (final LuceneQueryFactory<?> disjunct : disjuncts) {
            disjunctList.add(disjunct.createQuery(boost, dmqTieBreakerMultiplier, termQueryBuilder));
        }

        return new DisjunctionMaxQuery(disjunctList, dmqTieBreakerMultiplier);
      
    }


}
