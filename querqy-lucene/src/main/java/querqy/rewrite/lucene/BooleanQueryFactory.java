/**
 * 
 */
package querqy.rewrite.lucene;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;

/**
 * @author rene
 *
 */
public class BooleanQueryFactory implements LuceneQueryFactory<BooleanQuery> {
    
    private final float boost;
    protected final boolean disableCoord; 
    protected final LinkedList<Clause> clauses;
    protected final boolean normalizeBoost;
    
    
    public BooleanQueryFactory(float boost, boolean disableCoord, boolean normalizeBoost) {
        this.boost = boost;
        this.disableCoord = disableCoord;
        this.normalizeBoost = normalizeBoost;
        clauses = new LinkedList<>();
    }
    
    public void add(LuceneQueryFactory<?> factory, Occur occur) {
        clauses.add(new Clause(factory, occur));
    }
    
    public void add(Clause clause) {
        clauses.add(clause);
    }
    

    @Override
    public BooleanQuery createQuery(int dfToSet, IndexStats indexStats) throws IOException {
        BooleanQuery bq = new BooleanQuery(disableCoord);
        if (normalizeBoost) {
            int size = getNumberOfClauses();
            if (size > 0) {
                bq.setBoost(boost / (float) size);
            } else {
                bq.setBoost(boost);  
            }
        }
        
        for (Clause clause: clauses) {
            bq.add(clause.queryFactory.createQuery(dfToSet, indexStats), clause.occur);
        }
        return bq;
    }
    
    public int getNumberOfClauses() {
        return clauses.size();
    }
    
    public Clause getFirstClause() {
        return clauses.getFirst();
    }

    @Override
    public int getMaxDocFreqInSubtree(IndexStats indexStats) {
        int max = 0;
        for (Clause clause: clauses) {
            max = Math.max(max, clause.queryFactory.getMaxDocFreqInSubtree(indexStats));
        }
        return max;
    }

    public static class Clause {
        final Occur occur;
        final LuceneQueryFactory<?> queryFactory;
        
        public Clause(LuceneQueryFactory<?> queryFactory, Occur occur) {
            this.occur = occur;
            this.queryFactory = queryFactory;
        }
    }
}
