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
    public void prepareDocumentFrequencyCorrection(DocumentFrequencyAndTermContextProvider dftcp, boolean isBelowDMQ) {

        if ((!isBelowDMQ) && (dftcp != null)) {
            dftcp.newClause();
        }

        for (LuceneQueryFactory<?> disjunct : disjuncts) {
            disjunct.prepareDocumentFrequencyCorrection(dftcp, true);
        }

    }

    @Override
    public DisjunctionMaxQuery createQuery(FieldBoost boost, float dmqTieBreakerMultiplier, DocumentFrequencyAndTermContextProvider dftcp)
            throws IOException {

        List<Query> disjunctList = new LinkedList<>();

        for (LuceneQueryFactory<?> disjunct : disjuncts) {
            disjunctList.add(disjunct.createQuery(boost, dmqTieBreakerMultiplier, dftcp));
        }

        return new DisjunctionMaxQuery(disjunctList, dmqTieBreakerMultiplier);
      
    }


}
