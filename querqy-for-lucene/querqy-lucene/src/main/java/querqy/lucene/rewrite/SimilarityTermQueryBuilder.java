package querqy.lucene.rewrite;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Weight;

import java.io.IOException;
import java.util.Optional;

public class SimilarityTermQueryBuilder implements TermQueryBuilder {

    @Override
    public Optional<DocumentFrequencyCorrection> getDocumentFrequencyCorrection() {
        return Optional.empty();
    }

    @Override
    public SimilarityTermQuery createTermQuery(final Term term, final FieldBoost boost) {
        return new SimilarityTermQuery(term, boost);
    }

    public static class SimilarityTermQuery extends TermQuery {

        protected final FieldBoost fieldBoost;

        public SimilarityTermQuery(final Term t, final FieldBoost fieldBoost) {
            super(t);
            if (fieldBoost == null) {
                throw new IllegalArgumentException("FieldBoost must not be null");
            }
            this.fieldBoost = fieldBoost;
        }

        @Override
        public Weight createWeight(final IndexSearcher searcher, final boolean needsScores, final float boost)
                throws IOException {
            return super.createWeight(searcher, needsScores,
                    boost * fieldBoost.getBoost(getTerm().field(), searcher.getIndexReader()));
        }

        @Override
        public int hashCode() {
            return super.hashCode() ^ fieldBoost.hashCode();
        }

        @Override
        public boolean equals(final Object other) {

            if (!super.equals(other)) {
                return false;
            }

            return fieldBoost.equals(((SimilarityTermQuery) other).fieldBoost);

        }
    }
}
