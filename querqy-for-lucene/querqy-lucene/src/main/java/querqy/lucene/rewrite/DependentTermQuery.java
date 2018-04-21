/**
 * 
 */
package querqy.lucene.rewrite;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;

import java.io.IOException;
import java.util.Set;


/**
 * A TermQuery that depends on other term queries for the calculation of the document frequency
 * and/or the boost factor (field weight). 
 * 
 * @author René Kriegler, @renekrie
 *
 */
public class DependentTermQuery extends TermQuery {

    final int tqIndex;
    final DocumentFrequencyAndTermContextProvider dftcp;
    final FieldBoost fieldBoost;

    public DependentTermQuery(final Term term, final DocumentFrequencyAndTermContextProvider dftcp,
                              final FieldBoost fieldBoost) {
        this(term, dftcp, dftcp.termIndex(), fieldBoost);
    }

    protected DependentTermQuery(final Term term, final DocumentFrequencyAndTermContextProvider dftcp,
                                 final int tqIndex, final FieldBoost fieldBoost) {

        super(term);

        if (fieldBoost == null) {
            throw new IllegalArgumentException("FieldBoost must not be null");
        }

        if (dftcp == null) {
            throw new IllegalArgumentException("DocumentFrequencyAndTermContextProvider must not be null");
        }

        if (term == null) {
            throw new IllegalArgumentException("Term must not be null");
        }

        this.tqIndex  = tqIndex;
        this.dftcp = dftcp;
        this.fieldBoost = fieldBoost;
    }

    @Override
    public Weight createWeight(final IndexSearcher searcher, final boolean needsScores, final float boost) throws IOException {

        final DocumentFrequencyAndTermContextProvider.DocumentFrequencyAndTermContext dftc
                = dftcp.getDocumentFrequencyAndTermContext(tqIndex, searcher.getTopReaderContext());

        if (dftc.df < 1) {
            return new NeverMatchWeight();
        }

        return new TermWeight(searcher, needsScores, boost, dftc.termContext);

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

        if (!super.equals(obj)) {
            return false;
        }

        final DependentTermQuery other = (DependentTermQuery) obj;
        if (tqIndex != other.tqIndex)
            return false;
        if (!fieldBoost.equals(other.fieldBoost))
            return false;

        return true; // getTerm().equals(other.getTerm());  already assured in super class

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
        private final Similarity.SimWeight stats;
        private final TermContext termStates;
        private final boolean needsScores;
        private final float fieldBoostFactor;

        public TermWeight(final IndexSearcher searcher, final boolean needsScores, final float boost,
                          final TermContext termStates) throws IOException {

            super(DependentTermQuery.this);

            if (needsScores && termStates == null) {
                throw new IllegalStateException("termStates are required when scores are needed");
            }

            final Term term = getTerm();
            this.needsScores = needsScores;
            this.termStates = termStates;
            this.similarity = searcher.getSimilarity(needsScores);

            final int maxDoc = searcher.getIndexReader().maxDoc();
            final CollectionStatistics collectionStats = new CollectionStatistics(term.field(), maxDoc, -1, -1, -1);

            final TermStatistics termStats;
            if (needsScores) {
                termStats = searcher.termStatistics(term, termStates);
            } else {
                // we do not need the actual stats, use fake stats with docFreq=maxDoc and ttf=-1
                termStats = new TermStatistics(term.bytes(), maxDoc, -1);
            }

            fieldBoostFactor = fieldBoost.getBoost(getTerm().field(), searcher.getIndexReader());
            this.stats = similarity.computeWeight(boost * fieldBoostFactor, collectionStats, termStats);

        }

        @Override
        public String toString() { return "weight(" + DependentTermQuery.this + ")"; }


        @Override
        public Scorer scorer(LeafReaderContext context) throws IOException {

            assert termStates != null && termStates.wasBuiltFor(ReaderUtil.getTopLevelContext(context))
                    : "The top-reader used to create Weight is not the same as the current reader's top-reader: " + ReaderUtil.getTopLevelContext(context);
            final TermsEnum termsEnum = getTermsEnum(context);
            if (termsEnum == null) {
                return null;
            }
            PostingsEnum docs = termsEnum.postings(null, needsScores ? PostingsEnum.FREQS : PostingsEnum.NONE);
            assert docs != null;
            return new TermScorer(this, docs, similarity.simScorer(stats, context));

        }

        /**
         * Returns a {@link TermsEnum} positioned at this weights Term or null if
         * the term does not exist in the given context
         */
        private TermsEnum getTermsEnum(LeafReaderContext context) throws IOException {
            Term term = getTerm();
            if (termStates != null) {
                assert termStates.wasBuiltFor(ReaderUtil.getTopLevelContext(context))
                        : "The top-reader used to create Weight is not the same as the current reader's top-reader (" +
                        ReaderUtil.getTopLevelContext(context);

                final TermState state = termStates.get(context.ord);
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

        private boolean termNotInReader(LeafReader reader, Term term) throws IOException {
            // only called from assert
            //System.out.println("TQ.termNotInReader reader=" + reader + " term=" + field + ":" + bytes.utf8ToString());
            return reader.docFreq(term) == 0;
        }

        @Override
        public Explanation explain(LeafReaderContext context, int doc) throws IOException {
            Scorer scorer = scorer(context);
            if (scorer != null) {
                int newDoc = scorer.iterator().advance(doc);
                if (newDoc == doc) {
                    float freq = scorer.freq();
                    Similarity.SimScorer docScorer = similarity.simScorer(stats, context);
                    Explanation freqExplanation = Explanation.match(freq, "termFreq=" + freq);
                    Explanation scoreExplanation = docScorer.explain(doc, freqExplanation);
                    return Explanation.match(
                            scoreExplanation.getValue(),
                            "weight(" + getQuery() + " in " + doc + ") ["
                                    + similarity.getClass().getSimpleName() + ", " + fieldBoost.getClass().getSimpleName() + "], result of:",
                            scoreExplanation );
                }
            }
            return Explanation.noMatch("no matching term");
        }

        @Override
        public void extractTerms(Set<Term> terms) {
            terms.add(getTerm());
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
        public void extractTerms(Set<Term> terms) {
            terms.add(getTerm());
        }

    }

}
