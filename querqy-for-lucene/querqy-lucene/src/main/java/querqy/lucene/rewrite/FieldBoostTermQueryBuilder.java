package querqy.lucene.rewrite;

import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Weight;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

public class FieldBoostTermQueryBuilder implements TermQueryBuilder {


    @Override
    public Optional<DocumentFrequencyCorrection> getDocumentFrequencyCorrection() {
        return Optional.empty();
    }

    @Override
    public FieldBoostTermQuery createTermQuery(final Term term, final FieldBoost boost) {
        return new FieldBoostTermQuery(term, boost);
    }

    /**
     * A term query that scores by query queryBoost and {@link FieldBoost} but not by {@link org.apache.lucene.search.similarities.Similarity} or {@link DocumentFrequencyCorrection}.
     *
     * Created by rene on 11/09/2016.
     */
    public static class FieldBoostTermQuery extends TermQuery {

        protected final Term term;
        protected final FieldBoost fieldBoost;

        public FieldBoostTermQuery(final Term term, final FieldBoost fieldBoost) {

            super(term);

            this.term = term;

            if (fieldBoost == null) {
                throw new IllegalArgumentException("FieldBoost must not be null");
            }
            this.fieldBoost = fieldBoost;

        }

        @Override
        public Weight createWeight(final IndexSearcher searcher, final boolean needsScores, final float boost)
                throws IOException {
            final IndexReaderContext context = searcher.getTopReaderContext();
            final TermContext termState = TermContext.build(context, term);
            // TODO: set boosts to 1f if needsScores is false?
            return new FieldBoostWeight(termState, boost, fieldBoost.getBoost(term.field(), searcher.getIndexReader()));
        }



        class FieldBoostWeight extends Weight {
            private final TermContext termStates;
            private float score;
            private float queryBoost;
            private final float fieldBoost;


            public FieldBoostWeight(final TermContext termStates, final float queryBoost, final float fieldBoost) {
                super(FieldBoostTermQuery.this);
                assert termStates != null : "TermContext must not be null";
                this.termStates = termStates;

                this.queryBoost = queryBoost;
                this.fieldBoost = fieldBoost;
                this.score = queryBoost * fieldBoost;
            }

            float getScore() {
                return score;
            }

            @Override
            public String toString() {
                return "weight(" + FieldBoostTermQuery.this + ")";
            }



            @Override
            public Scorer scorer(final LeafReaderContext context) throws IOException {
                assert termStates != null && termStates.wasBuiltFor(ReaderUtil.getTopLevelContext(context))
                        : "The top-reader used to create Weight is not the same as the current reader's top-reader: " + ReaderUtil.getTopLevelContext(context);
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
            private TermsEnum getTermsEnum(final LeafReaderContext context) throws IOException {
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

            private boolean termNotInReader(final LeafReader reader, final Term term) throws IOException {
                // only called from assert
                // System.out.println("TQ.termNotInReader reader=" + reader + " term=" +
                // field + ":" + bytes.utf8ToString());
                return reader.docFreq(term) == 0;
            }

            @Override
            public Explanation explain(final LeafReaderContext context, final int doc) throws IOException {

                Scorer scorer = scorer(context);
                if (scorer != null) {
                    int newDoc = scorer.iterator().advance(doc);
                    if (newDoc == doc) {

                        Explanation scoreExplanation = Explanation.match(score, "product of:",
                                Explanation.match(queryBoost, "queryBoost"),
                                Explanation.match(fieldBoost, "fieldBoost")
                        );

                        Explanation result = Explanation.match(scorer.score(),
                                "weight(" + getQuery() + " in " + doc + ") ["
                                        + FieldBoostTermQuery.this.fieldBoost.getClass().getSimpleName() + "], result of:",
                                scoreExplanation

                        );



                        return result;
                    }
                }
                return Explanation.noMatch("no matching term");
            }

            public float getFieldBoost() {
                return fieldBoost;
            }

            @Override
            public void extractTerms(final Set<Term> terms) {
                terms.add(getTerm());
            }

            @Override
            public boolean isCacheable(LeafReaderContext ctx) {
                return true;
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
        public String toString(final String field) {
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
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            FieldBoostTermQuery that = (FieldBoostTermQuery) o;

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
}
