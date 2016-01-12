/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;

/**
 * @author rene
 *
 */
public class BooleanQueryFactory implements LuceneQueryFactory<Query> {

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
    public void prepareDocumentFrequencyCorrection(DocumentFrequencyCorrection dfc, boolean isBelowDMQ) {

        for (Clause clause : clauses) {
            clause.queryFactory.prepareDocumentFrequencyCorrection(dfc, isBelowDMQ);
        }

    }

    @Override
    public Query createQuery(FieldBoost boost, float dmqTieBreakerMultiplier, DocumentFrequencyCorrection dfc)
            throws IOException {

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.setDisableCoord(disableCoord);
      
        for (Clause clause : clauses) {
            builder.add(clause.queryFactory.createQuery(boost, dmqTieBreakerMultiplier, dfc), clause.occur);
        }

        Query bq = builder.build();

        if (normalizeBoost) {
            int size = getNumberOfClauses();
            if (size > 0) {
                bq = new BoostQuery(bq, 1f / (float) size);

            }
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
            if (occur == null) {
                throw new IllegalArgumentException("Occur must not be null");
            }
            this.occur = occur;
            this.queryFactory = queryFactory;
        }
    }

    public LinkedList<Clause> getClauses() {
        return clauses;
    }

}
