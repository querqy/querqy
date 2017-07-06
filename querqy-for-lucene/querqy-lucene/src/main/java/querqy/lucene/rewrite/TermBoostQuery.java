package querqy.lucene.rewrite;

import org.apache.lucene.index.*;
import org.apache.lucene.search.*;

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
        return new TermBoostWeight(termState, fieldBoost.getBoost(term.field(), searcher.getIndexReader()));
    }



    class TermBoostWeight extends Weight {
        private final TermContext termStates;
        private float unnormalizedScore;
        private float score;
        private float queryNorm;
        private float queryWeight;
        private float boost;


        public TermBoostWeight(TermContext termStates, float unnormalizedScore)
                throws IOException {
            super(TermBoostQuery.this);
            assert termStates != null : "TermContext must not be null";
            this.termStates = termStates;
            this.unnormalizedScore = unnormalizedScore;
            this.score = unnormalizedScore;
            normalize(1f, 1f);
        }

        @Override
        public String toString() {
            return "weight(" + TermBoostQuery.this + ")";
        }

        @Override
        public float getValueForNormalization() {
            return queryWeight * queryWeight;
        }

        @Override
        public void normalize(float queryNorm, float boost) {
            this.boost = boost;
            this.queryNorm = queryNorm;
            queryWeight = queryNorm * boost * unnormalizedScore;
            score = queryWeight;
        }

        @Override
        public Scorer scorer(LeafReaderContext context) throws IOException {

            assert termStates.topReaderContext == ReaderUtil.getTopLevelContext(context)
                    : "The top-reader used to create Weight (" + termStates.topReaderContext + ") is not the same as the current reader's top-reader (" + ReaderUtil.getTopLevelContext(context);

            final TermsEnum termsEnum = getTermsEnum(context);
            if (termsEnum == null) {
                return null;
            }
            PostingsEnum docs = termsEnum.postings(null, PostingsEnum.NONE);
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
            final TermsEnum termsEnum = context.reader().terms(term.field()).iterator();
            termsEnum.seekExact(term.bytes());
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

            Scorer scorer = scorer(context);
            if (scorer != null) {
                int newDoc = scorer.iterator().advance(doc);
                if (newDoc == doc) {

                    Explanation scoreExplanation = Explanation.match(score, "product of:",
                            Explanation.match(queryNorm, "queryNorm"),
                            Explanation.match(boost, "boost"),
                            Explanation.match(unnormalizedScore, "unnormalizedScore")
                    );

                    Explanation result = Explanation.match(scorer.score(),
                            "weight(" + getQuery() + " in " + doc + ") ["
                                    + fieldBoost.getClass().getSimpleName() + "], result of:",
                            scoreExplanation

                    );



                    return result;
                }
            }
            return Explanation.noMatch("no matching term");
        }

        public float getUnnormalizedScore() {
            return unnormalizedScore;
        }

        @Override
        public void extractTerms(Set<Term> terms) {
            terms.add(getTerm());
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

        @Override
        public DocIdSetIterator iterator() { return postingsEnum; }



        @Override
        public float score() throws IOException {
            assert docID() != DocIdSetIterator.NO_MORE_DOCS;
            return score;
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
