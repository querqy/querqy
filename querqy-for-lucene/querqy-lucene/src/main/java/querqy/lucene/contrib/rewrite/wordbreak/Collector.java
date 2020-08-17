package querqy.lucene.contrib.rewrite.wordbreak;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * <p>A Collector receives the de-compounding candidates, checks whether they exist in the index, and, optionally,
 * verifies that they co-occur in a document. It collects the candidates that match these requirements, ranks them and
 * keeps up to 'maxDecompoundExpansions' of them. The number of index lookups is restricted by the 'maxEvaluations'
 * property.</p>
 *
 * <p>Candidates are scores like this:
 *
 * The score depends on two main variables: A 'prior' score that reflects general the popularity of the morphological
 * structure in compound creation (see constants names PRIOR... in {@link GermanDecompoundingMorphology}), and a score
 * that depends on the document frequency (df) in the index of the two terms that form the compound. The df-dependent
 * score is calculated as:
 *
 *  <pre>
 *  score_df = -log(count(term1) / N) -log(count(term2) / N)
 *  </pre>
 *
 *  where a smaller value will be better.
 *
 *  To avoid issues with missing terms, we use add-1 smoothing:
 *
 *  <pre>
 *  score_df = -log((count(term1) +1) / (N + 1)) -log((count(term2) +1)/ (N + 1))
 *  </pre>
 *
 *  which can be reformulated into:
 *
 *  <pre>
 *  score_df = 2*log(N+1) - (log(count(term1) +1) + log(count(term2) +1))
 *  </pre>
 *
 *  We combine it with the score from the prior (score_prior) in a weighted manner:
 *
 *  <pre>
 *  score = score_prior^w / score_df^(1-w)
 *  </pre>
 *
 * </p>
 *
 *
 * <p>The approach to the calculation of score_df follows:
 * <ul>
 * <li>Schiller, A.: German compound analysis with wfsc. In Proceedings of Finite State Methods and Natural
 * Language Processing 2005, Helsinki (2005)</li>
 * <li>Marek, T.: Analysis of german compounds using weighted finite state transducers. Technical report, BA Thesis,
 * Universiät Tübingen (2006)</li>
 * <li>Both of the above quoted in: Alfonseca, E. &amp; Pharies, S.: German Decompounding in a Difficult Corpus.
 * CICLing 2008</li>
 * </ul>
 * </p>
 *
 * @author renekrie
 */
public class Collector {

    /**
     * A call to {@link #collect(CharSequence, CharSequence, Term, int, float)} returns a CollectionState, containing
     * the information about whether the maximum number of evaluations have been reached and if the terms could be found
     * in the index (fulfilling all requirements about verification and minimum index frequency).
     */
    enum CollectionState {

        MAX_EVALUATIONS_REACHED(null, true),
        MATCHED_MAX_EVALUATIONS_REACHED(true, true),
        MATCHED_MAX_EVALUATIONS_NOT_REACHED(true, false),
        NOT_MATCHED_MAX_EVALUATIONS_REACHED(false, true),
        NOT_MATCHED_MAX_EVALUATIONS_NOT_REACHED(false, false);

        private final Boolean matched;
        private final boolean maxEvaluationsReached;

        CollectionState(final Boolean matched, final boolean maxEvaluationsReached) {
            this.matched = matched;
            this.maxEvaluationsReached = maxEvaluationsReached;
        }

        boolean isMaxEvaluationsReached() {
            return maxEvaluationsReached;
        }

        Optional<Boolean> getMatched() {
            return Optional.ofNullable(matched);
        }
    }



    private final Queue<MorphologicalWordBreaker.BreakSuggestion> collection;
    private final int minSuggestionFrequency;
    private final boolean verifyCollation;
    private final IndexReader indexReader;
    private final String dictionaryField;
    private final float weightDfObservation;
    private final float totalDocsNorm;
    private final int maxDecompoundExpansions;
    private final IndexSearcher searcher;
    private final int maxEvaluations;
    private int evaluations = 0;

    /**
     *
     * @param minSuggestionFrequency Minimum frequency of each split term in the index
     * @param maxDecompoundExpansions Maximum number of decompound structures to return
     * @param maxEvaluations Maximum number of lookups in the index
     * @param verifyCollation Iff true, the compound parts must co-occur in a document in the index
     * @param indexReader The index reader
     * @param dictionaryField The document field to use for the lookup
     * @param weightDfObservation The weight of the observed document frequencies when combining with the score of the morphological compound pattern.
     */
    public Collector(final int minSuggestionFrequency,final int maxDecompoundExpansions, final int maxEvaluations,
                     final boolean verifyCollation, final IndexReader indexReader, final String dictionaryField,
                     final float weightDfObservation) {

        final int queueInitialCapacity = Math.min(maxDecompoundExpansions, 10);
        collection = new PriorityQueue<>(queueInitialCapacity);

        this.minSuggestionFrequency = minSuggestionFrequency;
        this.maxDecompoundExpansions = maxDecompoundExpansions;
        this.verifyCollation = verifyCollation;
        this.indexReader = indexReader;
        searcher = new IndexSearcher(indexReader);
        this.dictionaryField = dictionaryField;
        this.weightDfObservation = weightDfObservation;
        this.maxEvaluations = maxEvaluations;
        this.totalDocsNorm = 2f * (float) Math.log(1 + indexReader.numDocs());
    }


    /**
     *
     * @param left The modifier character sequence
     * @param right The head character sequence
     * @param rightTerm The head character sequence as a term in the dictionary field
     * @param rightDf The document frequency of the rightTerm
     * @param weightMorphologicalPattern The weight of this specific morphological pattern.
     * @return The state of candidate collection
     */
    public CollectionState collect(final CharSequence left, final CharSequence right, final Term rightTerm,
                                   final int rightDf, final float weightMorphologicalPattern) {

        if (maxEvaluations <= evaluations) {
            return CollectionState.MAX_EVALUATIONS_REACHED;
        }
        evaluations++;

        final Term leftTerm = new Term(dictionaryField, new BytesRef(left));
        final int leftDf;
        try {
            leftDf = indexReader.docFreq(leftTerm);
            if (leftDf >= minSuggestionFrequency) {

                final float score = weightDfObservation == 0f ? weightMorphologicalPattern
                        : weightMorphologicalPattern /
                            ((float) Math.pow(totalDocsNorm - Math.log(leftDf + 1) - Math.log(rightDf + 1),
                                    weightDfObservation));

                if (verifyCollation) {

                    if (((collection.size() < maxDecompoundExpansions) || (score > collection.element().score))
                            && hasMinMatches(1, leftTerm, rightTerm)) {
                        collection.offer(new MorphologicalWordBreaker.BreakSuggestion(new CharSequence[]{left, right},
                                score));

                        if (collection.size() > maxDecompoundExpansions) {
                            collection.poll();
                        }
                        return evaluations == maxEvaluations
                                ? CollectionState.MATCHED_MAX_EVALUATIONS_REACHED
                                : CollectionState.MATCHED_MAX_EVALUATIONS_NOT_REACHED;
                    }

                } else {

                    collection.offer(new MorphologicalWordBreaker.BreakSuggestion(new CharSequence[]{left, right},
                            score));
                    if (collection.size() > maxDecompoundExpansions) {
                        collection.poll();
                    }
                    return evaluations == maxEvaluations
                            ? CollectionState.MATCHED_MAX_EVALUATIONS_REACHED
                            : CollectionState.MATCHED_MAX_EVALUATIONS_NOT_REACHED;
                }

            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return evaluations == maxEvaluations
                ? CollectionState.NOT_MATCHED_MAX_EVALUATIONS_REACHED
                : CollectionState.NOT_MATCHED_MAX_EVALUATIONS_NOT_REACHED;

    }

    public boolean maxEvaluationsReached() {
        return evaluations >= maxEvaluations;
    }

    /**
     * Get the collected results ordered by decreasing score. This resets the internal result queue.
     *
     * @return The collected results.
     */
    public List<CharSequence[]> flushResults() {
        if (collection.isEmpty()) {
            return Collections.emptyList();
        }

        final LinkedList<CharSequence[]> result = new LinkedList<>();
        while (collection.size() > 0) {
            result.addFirst(collection.remove().sequence);
        }

        return result;
    }

    private boolean hasMinMatches(final int minCount, final Term term1, final Term term2)
            throws IOException {


        final IndexReaderContext topReaderContext = searcher.getTopReaderContext();
        final IndexReader indexReader = topReaderContext.reader();
        // TODO: deleted documents?
        final int numDocs = indexReader.numDocs();
        if (minCount > numDocs) {
            return false;
        }

        final int df1 = indexReader.docFreq(term1);
        if (minCount > df1) {
            return false;
        }

        final int df2 = indexReader.docFreq(term2);
        if (minCount > df2) {
            return false;
        }

        int count = 0;

        for (final LeafReaderContext context : topReaderContext.leaves()) {

            final Terms terms1 = context.reader().terms(term1.field());
            final Terms terms2 = context.reader().terms(term2.field());

            final TermsEnum termsEnum1 = terms1.iterator();
            if (!termsEnum1.seekExact(term1.bytes())) {
                continue;
            }

            final TermsEnum termsEnum2 = terms2.iterator();
            if (!termsEnum2.seekExact(term2.bytes())) {
                continue;
            }

            final PostingsEnum postings1 = termsEnum1.postings(null, PostingsEnum.NONE);
            final PostingsEnum postings2 = termsEnum2.postings(null, PostingsEnum.NONE);

            int doc1 = postings1.nextDoc();
            while (doc1 != DocIdSetIterator.NO_MORE_DOCS) {
                int doc2 = postings2.advance(doc1);
                if (doc2 == DocIdSetIterator.NO_MORE_DOCS) {
                    break;
                }
                if (doc2 == doc1) {
                    count++;
                    if (count >= minCount) {
                        return true;
                    }
                } else if (doc2 > doc1) {
                    doc1 = postings1.advance(doc2);
                    if (doc2 == doc1) {
                        count++;
                        if (count >= minCount) {
                            return true;
                        }
                    }
                }
            }

        }

        return false;

    }

}
