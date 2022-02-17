/**
 * 
 */
package querqy.lucene.rewrite;

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
    protected float tieBreaker;
   
    public DisjunctionMaxQueryFactory(final float tieBreaker) {
        this.tieBreaker = tieBreaker;
        disjuncts = new LinkedList<>();
    }

    public DisjunctionMaxQueryFactory(final List<LuceneQueryFactory<?>> disjuncts, final float tieBreaker) {
        this.disjuncts = new LinkedList<>(disjuncts);
        this.tieBreaker = tieBreaker;
    }

    public void add(LuceneQueryFactory<?> disjunct) {
       disjuncts.add(disjunct);
   }

    public final int getNumberOfDisjuncts() {
       return disjuncts.size();
    }

    public LuceneQueryFactory<?> getFirstDisjunct() {
       return disjuncts.getFirst();
   }

    public void setTieBreaker(final float tieBreaker) {
        this.tieBreaker = tieBreaker;
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
    public DisjunctionMaxQuery createQuery(final FieldBoost boost, final TermQueryBuilder termQueryBuilder) {

        final List<Query> disjunctList = new LinkedList<>();

        for (final LuceneQueryFactory<?> disjunct : disjuncts) {
            disjunctList.add(disjunct.createQuery(boost, termQueryBuilder));
        }

        return new DisjunctionMaxQuery(disjunctList, tieBreaker);
      
    }

    @Override
    public <R> R accept(final LuceneQueryFactoryVisitor<R> visitor) {
        return visitor.visit(this);
    }


}
