package querqy.lucene.rewrite;

import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Bits;

import java.io.IOException;
import java.util.Set;

/**
 * A term query that scores by query boost and FieldBoost but not by similarity.
 *
 * Created by rene on 11/09/2016.
 */
public class TermBoostQuery extends TermQuery {

    protected final Term term;
    protected final FieldBoost fieldBoost;

    public TermBoostQuery(Term term, FieldBoost fieldBoost) {

        super(term);

        if (term == null) {
            throw new IllegalArgumentException("Term must not be null");
        }
        this.term = term;

        if (fieldBoost == null) {
            throw new IllegalArgumentException("FieldBoost must not be null");
        }
        this.fieldBoost = fieldBoost;

    }

    @Override
    public Weight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException {
        final IndexReaderContext context = searcher.getTopReaderContext();
        final TermContext termState = TermContext.build(context, term);
        return new TermBoostWeight(termState, fieldBoost.getBoost(term.field(), searcher) * getBoost());
    }

    @Override
    public void extractTerms(Set<Term> terms) {
        terms.add(getTerm());
    }

    class TermBoostWeight extends Weight {
        private final TermContext termStates;
        private float unnormalizedScore;
        private float norm = 1f;
        private float score;


        public TermBoostWeight(TermContext termStates, float unnormalizedScore)
                throws IOException {
            super(TermBoostQuery.this);
            assert termStates != null : "TermContext must not be null";
            this.termStates = termStates;
            this.unnormalizedScore = unnormalizedScore;
        }

        @Override
        public String toString() {
            return "weight(" + TermBoostQuery.this + ")";
        }

        @Override
        public float getValueForNormalization() {
            return unnormalizedScore * unnormalizedScore;
        }

        @Override
        public void normalize(float queryNorm, float topLevelBoost) {
            this.norm = queryNorm * topLevelBoost;
            score = unnormalizedScore * this.norm;
        }

        @Override
        public Scorer scorer(LeafReaderContext context, Bits acceptDocs) throws IOException {
            assert termStates.topReaderContext
                    == ReaderUtil.getTopLevelContext(context)
                    : "The top-reader used to create Weight (" + termStates.topReaderContext + ") is not the same as the current reader's top-reader (" + ReaderUtil.getTopLevelContext(context);
            final TermsEnum termsEnum = getTermsEnum(context);
            if (termsEnum == null) {
                return null;
            }
            PostingsEnum docs = termsEnum.postings(acceptDocs, null, PostingsEnum.NONE);
            assert docs != null;
            return new TermBoostScorer(this, docs, score);
        }

        /**
         * Returns a {@link TermsEnum} positioned at this weights Term or null if
         * the term does not exist in the given context
         */
        private TermsEnum getTermsEnum(LeafReaderContext context) throws IOException {
            final TermState state = termStates.get(context.ord);
            if (state == null) { // term is not present in that reader
                assert termNotInReader(context.reader(), term) : "no termstate found but term exists in reader term=" + term;
                return null;
            }
            // System.out.println("LD=" + reader.getLiveDocs() + " set?=" +
            // (reader.getLiveDocs() != null ? reader.getLiveDocs().get(0) : "null"));
            final TermsEnum termsEnum = context.reader().terms(term.field())
                    .iterator(null);
            termsEnum.seekExact(term.bytes(), state);
            return termsEnum;
        }

        private boolean termNotInReader(LeafReader reader, Term term) throws IOException {
            // only called from assert
            // System.out.println("TQ.termNotInReader reader=" + reader + " term=" +
            // field + ":" + bytes.utf8ToString());
            return reader.docFreq(term) == 0;
        }

        @Override
        public Explanation explain(LeafReaderContext context, int doc) throws IOException {
            Scorer scorer = scorer(context, context.reader().getLiveDocs());
            if (scorer != null) {
                int newDoc = scorer.advance(doc);
                if (newDoc == doc) {
                    ComplexExplanation result = new ComplexExplanation();
                    result.setDescription("weight(" + getQuery() + " in " + doc + ") ["
                            + fieldBoost.getClass().getSimpleName() + "], result of:");
                    Explanation scoreExplanation = new Explanation(score, "product of:");
                    scoreExplanation.addDetail(new Explanation(norm, "norm"));
                    scoreExplanation.addDetail(new Explanation(unnormalizedScore, "boost"));
                    result.addDetail(scoreExplanation);
                    result.setValue(scoreExplanation.getValue());
                    result.setMatch(true);
                    return result;
                }
            }
            return new ComplexExplanation(false, 0.0f, "no matching term");
        }

        public float getUnnormalizedScore() {
            return unnormalizedScore;
        }

    }

    class TermBoostScorer extends Scorer {
        private final PostingsEnum postingsEnum;
        private final float score;

        /**
         * Construct a <code>TermScorer</code>.
         *
         * @param weight
         *          The weight of the <code>Term</code> in the query.
         * @param td
         *          An iterator over the documents matching the <code>Term</code>.
         * @param score
         *          The score
         */
        TermBoostScorer(final Weight weight, final PostingsEnum td, final float score) {
            super(weight);
            this.score = score;
            this.postingsEnum = td;
        }

        @Override
        public int docID() {
            return postingsEnum.docID();
        }

        @Override
        public int freq() throws IOException {
            return 1;
        }

        /**
         * Advances to the next document matching the query. <br>
         *
         * @return the document matching the query or NO_MORE_DOCS if there are no more documents.
         */
        @Override
        public int nextDoc() throws IOException {
            return postingsEnum.nextDoc();
        }

        @Override
        public float score() throws IOException {
            assert docID() != NO_MORE_DOCS;
            return score;
        }

        /**
         * Advances to the first match beyond the current whose document number is
         * greater than or equal to a given target. <br>
         * The implementation uses {@link org.apache.lucene.index.PostingsEnum#advance(int)}.
         *
         * @param target
         *          The target document number.
         * @return the matching document or NO_MORE_DOCS if none exist.
         */
        @Override
        public int advance(int target) throws IOException {
            return postingsEnum.advance(target);
        }

        @Override
        public long cost() {
            return postingsEnum.cost();
        }

        /** Returns a string representation of this <code>TermScorer</code>. */
        @Override
        public String toString() { return "scorer(" + weight + ")[" + super.toString() + "]"; }
    }



    @Override
    public String toString(String field) {
        StringBuilder buffer = new StringBuilder();
        if (!term.field().equals(field)) {
            buffer.append(term.field());
            buffer.append(":");
        }
        buffer.append(term.text());
        buffer.append(fieldBoost.toString(term.field()));
        return buffer.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        TermBoostQuery that = (TermBoostQuery) o;

        if (!term.equals(that.term)) return false;
        return fieldBoost.equals(that.fieldBoost);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + term.hashCode();
        result = 31 * result + fieldBoost.hashCode();
        return result;
    }
}
