package querqy.lucene.rewrite;

import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.TermStates;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafSimScorer;
import org.apache.lucene.search.Matches;
import org.apache.lucene.search.MatchesIterator;
import org.apache.lucene.search.MatchesUtils;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermScorer;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;

import java.io.IOException;
import java.util.Optional;

/**
 * This {@link TermQueryBuilder} creates a {@link DependentTermQuery}, which takes part in {@link DocumentFrequencyCorrection}
 * and thus depends on other {@link TermQuery}s for scoring.
 */
public class DependentTermQueryBuilder implements TermQueryBuilder {

    protected final DocumentFrequencyCorrection dfc;

    public DependentTermQueryBuilder(final DocumentFrequencyCorrection dfc) {
        this.dfc = dfc;
    }


    @Override
    public Optional<DocumentFrequencyCorrection> getDocumentFrequencyCorrection() {
        return Optional.of(dfc);
    }

    @Override
    public DependentTermQuery createTermQuery(final Term term, final FieldBoost boost) {
        return new DependentTermQuery(term, dfc, boost);
    }

    /**
     * A TermQuery that depends on other term queries for the calculation of the document frequency
     * and/or the boost factor (field weight).
     *
     * @author René Kriegler, @renekrie
     *
     */
    public static class DependentTermQuery extends TermQuery {

        final int tqIndex;
        final DocumentFrequencyCorrection dftcp;
        final FieldBoost fieldBoost;

        public DependentTermQuery(final Term term, final DocumentFrequencyCorrection dftcp,
                                  final FieldBoost fieldBoost) {
            this(term, dftcp, dftcp.termIndex(), fieldBoost);
        }

        protected DependentTermQuery(final Term term, final DocumentFrequencyCorrection dftcp,
                                     final int tqIndex, final FieldBoost fieldBoost) {

            super(term);

            if (fieldBoost == null) {
                throw new IllegalArgumentException("FieldBoost must not be null");
            }

            if (dftcp == null) {
                throw new IllegalArgumentException("DocumentFrequencyAndTermContextProvider must not be null");
            }

            this.tqIndex  = tqIndex;
            this.dftcp = dftcp;
            this.fieldBoost = fieldBoost;
        }

        @Override
        public Weight createWeight(final IndexSearcher searcher, final ScoreMode scoreMode, final float boost) throws IOException {

            final DocumentFrequencyCorrection.DocumentFrequencyAndTermContext dftc
                    = dftcp.getDocumentFrequencyAndTermContext(tqIndex, searcher.getTopReaderContext());

            if (dftc.df < 1) {
                return new NeverMatchWeight();
            }

            return new TermWeight(searcher, scoreMode, boost, dftc.termStates);

        }

        @Override
        public void visit(final QueryVisitor visitor) {
            final Term term = getTerm();
            if (visitor.acceptField(term.field())) {
                visitor.consumeTerms(this, term);
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = prime  + tqIndex;
            result = prime * result + fieldBoost.hashCode();
           // result = prime * result + getTerm().hashCode(); handled in super class
            return super.hashCode() ^ result;
        }

        @Override
        public boolean equals(final Object obj) {

            if (obj == null) {
                return false;
            }

            if (!(obj instanceof DependentTermQuery)) {
                return false;
            }

            final DependentTermQuery other = (DependentTermQuery) obj;
            final Term term = getTerm();

            if (!term.equals(other.getTerm())) {
                return false;
            }

            if (tqIndex != other.tqIndex)
                return false;

            return fieldBoost.equals(other.fieldBoost);

        }

        @Override
        public String toString(final String field) {
            final Term term = getTerm();
            final StringBuilder buffer = new StringBuilder();
            if (!term.field().equals(field)) {
              buffer.append(term.field());
              buffer.append(":");
            }
            buffer.append(term.text());
            buffer.append(fieldBoost.toString(term.field()));
            return buffer.toString();

        }

        public FieldBoost getFieldBoost() {
            return fieldBoost;
        }


        /**
         * Copied from inner class in {@link TermQuery}
         *
         */
        final class TermWeight extends Weight {
            private final Similarity similarity;
            private final Similarity.SimScorer simScorer;
            private final TermStates termStates;
            private final ScoreMode scoreMode;

            public TermWeight(final IndexSearcher searcher, final ScoreMode scoreMode,
                              float boost, final TermStates termStates) throws IOException {
                super(DependentTermQuery.this);
                if (scoreMode.needsScores() && termStates == null) {
                    throw new IllegalStateException("termStates are required when scores are needed");
                }
                final Term term = getTerm();
                this.scoreMode = scoreMode;
                this.termStates = termStates;
                this.similarity = searcher.getSimilarity();

                final int df = termStates.docFreq();

                final CollectionStatistics collectionStats;
                final TermStatistics termStats;
                if (scoreMode.needsScores()) {

                    final CollectionStatistics trueCollectionStats = searcher.collectionStatistics(term.field());
                    final long maxDoc = Math.max(df, trueCollectionStats.maxDoc());
                    final long sumTotalTermFreq = Math.max(trueCollectionStats.sumTotalTermFreq(),
                            termStates.totalTermFreq());

                    final long sumDocFreq = Math.max(maxDoc, trueCollectionStats.sumDocFreq());

                    collectionStats = new CollectionStatistics(term.field(), maxDoc, maxDoc,
                            Math.max(sumTotalTermFreq, sumDocFreq), sumDocFreq);

                    termStats =
                            termStates.docFreq() > 0
                                    ? searcher.termStatistics(term, df, termStates.totalTermFreq())
                                    : null;

                } else {
                    // we do not need the actual stats, use fake stats with docFreq=maxDoc=ttf=1
                    collectionStats = new CollectionStatistics(term.field(), 1, 1, 1, 1);
                    termStats = new TermStatistics(term.bytes(), 1, 1);
                }

                if (termStats == null) {
                    this.simScorer = null; // term doesn't exist in any segment, we won't use similarity at all
                } else {
                    // We've modelled field boosting in a FieldBoost implementation so that for example
                    // field boosts can also depend on the term distribution over fields. Calculate the field boost
                    // using that FieldBoost model and multiply with the general boost
                    final float fieldBoostFactor = fieldBoost.getBoost(term.field(), searcher.getIndexReader());
                    this.simScorer = similarity.scorer(boost * fieldBoostFactor, collectionStats, termStats);
                }
            }


            @Override
            public String toString() { return "weight(" + DependentTermQuery.this + ")"; }

            @Override
            public boolean isCacheable(LeafReaderContext ctx) {
                return true;
            }

            @Override
            public Matches matches(LeafReaderContext context, int doc) throws IOException {
                TermsEnum te = getTermsEnum(context);
                if (te == null) {
                    return null;
                }
                return MatchesUtils.forField(
                        getTerm().field(),
                        () -> {
                            PostingsEnum pe = te.postings(null, PostingsEnum.OFFSETS);
                            if (pe.advance(doc) != doc) {
                                return null;
                            }
                            return new TermMatchesIterator(getQuery(), pe);
                        });
            }

            @Override
            public TermScorer scorer(final LeafReaderContext context) throws IOException {

                assert termStates == null || termStates.wasBuiltFor(ReaderUtil.getTopLevelContext(context))
                        : "The top-reader used to create Weight is not the same as the current reader's top-reader ("
                        + ReaderUtil.getTopLevelContext(context);

                final TermsEnum termsEnum = getTermsEnum(context);
                if (termsEnum == null) {
                    return null;
                }

                LeafSimScorer scorer = new LeafSimScorer(simScorer, context.reader(), getTerm().field(),
                        scoreMode.needsScores());
                if (scoreMode == ScoreMode.TOP_SCORES) {
                    return new TermScorer(this, termsEnum.impacts(PostingsEnum.FREQS), scorer);
                } else {
                    return new TermScorer(this, termsEnum.postings(null, scoreMode.needsScores()
                            ? PostingsEnum.FREQS : PostingsEnum.NONE), scorer);
                }

            }

            /**
             * Returns a {@link TermsEnum} positioned at this weights Term or null if
             * the term does not exist in the given context
             */
            private TermsEnum getTermsEnum(final LeafReaderContext context) throws IOException {
                final Term term = getTerm();
                if (termStates != null) {
                    assert termStates.wasBuiltFor(ReaderUtil.getTopLevelContext(context))
                            : "The top-reader used to create Weight is not the same as the current reader's top-reader (" +
                            ReaderUtil.getTopLevelContext(context);

                    final TermState state = termStates.get(context);
                    if (state == null) { // term is not present in that reader
                        assert termNotInReader(context.reader(), term) : "no termstate found but term exists in reader term=" + term;
                        return null;
                    }
                    //System.out.println("LD=" + reader.getLiveDocs() + " set?=" + (reader.getLiveDocs() != null ? reader.getLiveDocs().get(0) : "null"));
                    final TermsEnum termsEnum = context.reader().terms(term.field()).iterator();
                    termsEnum.seekExact(term.bytes(), state);
                    return termsEnum;
                } else {
                    // TermQuery used as a filter, so the term states have not been built up front
                    final Terms terms = context.reader().terms(term.field());
                    if (terms == null) {
                        return null;
                    }
                    final TermsEnum termsEnum = terms.iterator();
                    if (termsEnum.seekExact(term.bytes())) {
                        return termsEnum;
                    } else {
                        return null;
                    }
                }
            }

            private boolean termNotInReader(final LeafReader reader, final Term term) throws IOException {
                return reader.docFreq(term) == 0;
            }

            @Override
            public Explanation explain(final LeafReaderContext context, final int doc) throws IOException {

                TermScorer scorer = scorer(context);
                if (scorer != null) {
                    int newDoc = scorer.iterator().advance(doc);
                    if (newDoc == doc) {
                        float freq = scorer.freq();
                        LeafSimScorer docScorer = new LeafSimScorer(simScorer, context.reader(), getTerm().field(), true);
                        Explanation freqExplanation = Explanation.match(freq, "freq, occurrences of term within document");
                        Explanation scoreExplanation = docScorer.explain(doc, freqExplanation);
                        return Explanation.match(
                                scoreExplanation.getValue(),
                                "weight(" + getQuery() + " in " + doc + ") ["
                                        + similarity.getClass().getSimpleName() + "], result of:",
                                scoreExplanation);
                    }
                }
                return Explanation.noMatch("no matching term");
            }

        }

        public class NeverMatchWeight extends Weight {

            protected NeverMatchWeight() {
                super(DependentTermQuery.this);
            }

            @Override
            public Explanation explain(LeafReaderContext context, int doc)
                    throws IOException {
                return Explanation.noMatch("no matching term");
            }

            @Override
            public Scorer scorer(LeafReaderContext context)
                    throws IOException {
                return null;
            }

            @Override
            public boolean isCacheable(LeafReaderContext ctx) {
                return true;
            }
        }

    }

    /**
     * Copied from org.apache.lucene.search.TermMatchesIterator
     */
    static class TermMatchesIterator implements MatchesIterator {

        private int upto;
        private int pos;
        private final PostingsEnum pe;
        private final Query query;

        TermMatchesIterator(Query query, PostingsEnum pe) throws IOException {
            this.pe = pe;
            this.query = query;
            this.upto = pe.freq();
        }

        @Override
        public boolean next() throws IOException {
            if (upto-- > 0) {
                pos = pe.nextPosition();
                return true;
            }
            return false;
        }

        @Override
        public int startPosition() {
            return pos;
        }

        @Override
        public int endPosition() {
            return pos;
        }

        @Override
        public int startOffset() throws IOException {
            return pe.startOffset();
        }

        @Override
        public int endOffset() throws IOException {
            return pe.endOffset();
        }

        @Override
        public MatchesIterator getSubMatches() throws IOException {
            return null;
        }

        @Override
        public Query getQuery() {
            return query;
        }

    }
}
