/**
 * 
 */
package querqy.rewrite.lucene;

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
    public TermQuery createQuery(int dfToSet, IndexStats indexStats) {
        TermQuery tq = dfToSet < 1 ? new TermQuery(term) : new TermQuery(term, dfToSet);
        tq.setBoost(boost);
        return tq;
    }

    @Override
    public int getMaxDocFreqInSubtree(IndexStats indexStats) {
        return indexStats.df(term);
    }

}
