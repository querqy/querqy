/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.Similarity.SimScorer;

import querqy.lucene.rewrite.DocumentFrequencyCorrection.DocumentFrequencyAndTermContext;

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

    public DependentTermQuery(Term term, DocumentFrequencyAndTermContextProvider dftcp, FieldBoost fieldBoost) {
        this(term, dftcp, dftcp.termIndex(), fieldBoost);
    }

    protected DependentTermQuery(Term term, DocumentFrequencyAndTermContextProvider dftcp, int tqIndex, FieldBoost fieldBoost) {

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
    public Weight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException {
        DocumentFrequencyAndTermContext dftc = dftcp.getDocumentFrequencyAndTermContext(tqIndex, searcher);
        if (dftc.df < 1) {
            return new NeverMatchWeight();
        }
        return new TermWeight(searcher, needsScores, dftc.termContext);
        
    }
    

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = prime  + dftcp.hashCode();
        result = prime * result + fieldBoost.hashCode();
        result = prime * result + getTerm().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        DependentTermQuery other = (DependentTermQuery) obj;
        if (!dftcp.equals(other.dftcp))
            return false;
        if (!fieldBoost.equals(other.fieldBoost))
            return false;
        return getTerm().equals(other.getTerm());
    }
    
    @Override
    public String toString(String field) {
        Term term = getTerm();
        StringBuilder buffer = new StringBuilder();
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
    
    @Override
    public Query rewrite(IndexReader reader) throws IOException {
        float boost = fieldBoost.getBoost(getTerm().field(), reader);
        if (boost != 1f) {
            DependentTermQuery clone = new DependentTermQuery(getTerm(), dftcp, tqIndex, ConstantFieldBoost.NORM_BOOST);
            return new BoostQuery(clone, boost);
        }
        return this;
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
        
        public TermWeight(IndexSearcher searcher, boolean needsScores, TermContext termStates)
          throws IOException {
            super(DependentTermQuery.this);
            Term term = getTerm();
            this.needsScores = needsScores;
            assert termStates != null : "TermContext must not be null";
            this.termStates = termStates;
            this.similarity = searcher.getSimilarity(needsScores);
            this.stats = similarity.computeWeight(
              searcher.collectionStatistics(term.field()), 
              searcher.termStatistics(term, termStates));
        }

        @Override
        public String toString() { return "weight(" + DependentTermQuery.this + ")"; }


        @Override
        public float getValueForNormalization() {
          return stats.getValueForNormalization();
        }

        @Override
        public void normalize(float queryNorm, float topLevelBoost) {
          stats.normalize(queryNorm, topLevelBoost);
        }

        @Override
        public Scorer scorer(LeafReaderContext context) throws IOException {
            
            assert termStates.topReaderContext == ReaderUtil.getTopLevelContext(context) : "The top-reader used to create Weight (" + termStates.topReaderContext + ") is not the same as the current reader's top-reader (" + ReaderUtil.getTopLevelContext(context);
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
          final TermState state = termStates.get(context.ord);
          if (state == null) { // term is not present in that reader
            assert termNotInReader(context.reader(), term) : "no termstate found but term exists in reader term=" + term;
            return null;
          }
          //System.out.println("LD=" + reader.getLiveDocs() + " set?=" + (reader.getLiveDocs() != null ? reader.getLiveDocs().get(0) : "null"));
          final TermsEnum termsEnum = context.reader().terms(term.field()).iterator();
          termsEnum.seekExact(term.bytes(), state);
          return termsEnum;
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
              int newDoc = scorer.advance(doc);
              if (newDoc == doc) {
                float freq = scorer.freq();
                SimScorer docScorer = similarity.simScorer(stats, context);
                Explanation freqExplanation = Explanation.match(freq, "termFreq=" + freq);
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
        public float getValueForNormalization() throws IOException {
            return 1f;
        }

        @Override
        public void normalize(float norm, float topLevelBoost) {
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
