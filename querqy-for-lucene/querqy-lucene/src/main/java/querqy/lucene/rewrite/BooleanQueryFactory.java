/**
 * 
 */
package querqy.lucene.rewrite;

import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;

/**
 * @author rene
 *
 */
public class BooleanQueryFactory implements LuceneQueryFactory<Query> {

    protected final LinkedList<Clause> clauses;
    protected final boolean normalizeBoost;

    public BooleanQueryFactory(final boolean normalizeBoost) {
        this.normalizeBoost = normalizeBoost;
        clauses = new LinkedList<>();
    }

    public BooleanQueryFactory(final List<Clause> clauses, final boolean normalizeBoost) {
        this(new LinkedList<>(clauses), normalizeBoost);
    }

    public BooleanQueryFactory(final LinkedList<Clause> clauses, final boolean normalizeBoost) {
        this.normalizeBoost = normalizeBoost;
        this.clauses = clauses;
    }

    public void add(final LuceneQueryFactory<?> factory, Occur occur) {
        clauses.add(new Clause(factory, occur));
    }

    public void add(final Clause clause) {
        clauses.add(clause);
    }

    @Override
    public void prepareDocumentFrequencyCorrection(final DocumentFrequencyCorrection dfc,
                                                   final boolean isBelowDMQ) {

        for (final Clause clause : clauses) {
            clause.queryFactory.prepareDocumentFrequencyCorrection(dfc, isBelowDMQ);
        }

    }

    @Override
    public Query createQuery(final FieldBoost boost, final TermQueryBuilder termQueryBuilder) {

        final BooleanQuery.Builder builder = new BooleanQuery.Builder();

        for (final Clause clause : clauses) {
            builder.add(clause.queryFactory.createQuery(boost, termQueryBuilder), clause.occur);
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

    @Override
    public <R> R accept(final LuceneQueryFactoryVisitor<R> visitor) {
        return visitor.visit(this);
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

        public Clause(final LuceneQueryFactory<?> queryFactory, final Occur occur) {
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
