/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;

/**
 * @author rene
 *
 */
public class BooleanQueryFactory implements LuceneQueryFactory<BooleanQuery> {

    protected final boolean disableCoord;
    protected final LinkedList<Clause> clauses;
    protected final boolean normalizeBoost;

    public BooleanQueryFactory(boolean disableCoord, boolean normalizeBoost) {
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
    public BooleanQuery createQuery(FieldBoost boost, float dmqTieBreakerMultiplier, DocumentFrequencyCorrection dfc, boolean isBelowDMQ) throws IOException {
        BooleanQuery bq = new BooleanQuery(disableCoord);
      
        if (normalizeBoost) {
            int size = getNumberOfClauses();
            if (size > 0) {
                bq.setBoost(1f / (float) size);
            } else {
                bq.setBoost(1f);
            }
        } else {
            bq.setBoost(1f);
        }

        for (Clause clause : clauses) {
            bq.add(clause.queryFactory.createQuery(boost, dmqTieBreakerMultiplier, dfc, isBelowDMQ), clause.occur);
        }
      
        return bq;
    }

    public int getNumberOfClauses() {
        return clauses.size();
    }

    public Clause getFirstClause() {
        return clauses.getFirst();
    }

    public static class Clause {
        final Occur occur;
        final LuceneQueryFactory<?> queryFactory;

        public Clause(LuceneQueryFactory<?> queryFactory, Occur occur) {
            this.occur = occur;
            this.queryFactory = queryFactory;
        }
    }

    public LinkedList<Clause> getClauses() {
        return clauses;
    }

}
