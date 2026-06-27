/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Querqy Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package querqy.rewrite.wordbreak;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * <p>A Collector receives the de-compounding candidates, checks whether they exist in the corpus, and, optionally,
 * verifies that they co-occur in a document. It collects the candidates that match these requirements, ranks them and
 * keeps up to 'maxDecompoundExpansions' of them. The number of corpus lookups is restricted by the 'maxEvaluations'
 * property.</p>
 *
 * <p>Candidates are scored like this:</p>
 *
 * The score depends on two main variables: A 'prior' score that reflects general the popularity of the morphological
 * structure in compound creation (see constants names PRIOR... in {@link GermanDecompoundingMorphology}), and a score
 * that depends on the document frequency (df) in the corpus of the two terms that form the compound. The df-dependent
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
 *
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
 *
 * @author renekrie
 */
public class Collector {

    /**
     * A call to {@link #collect(CharSequence, CharSequence, int, float)} returns a CollectionState, containing
     * the information about whether the maximum number of evaluations have been reached and if the terms could be found
     * in the corpus (fulfilling all requirements about verification and minimum frequency).
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



    private final Queue<Suggestion> collection;
    private final int minSuggestionFrequency;
    private final boolean verifyCollation;
    private final TermCorpus termCorpus;
    private final float weightDfObservation;
    private final float totalDocsNorm;
    private final int maxDecompoundExpansions;
    private final int maxEvaluations;
    private int evaluations = 0;

    /**
     *
     * @param minSuggestionFrequency Minimum frequency of each split term in the corpus
     * @param maxDecompoundExpansions Maximum number of decompound structures to return
     * @param maxEvaluations Maximum number of lookups in the corpus
     * @param verifyCollation Iff true, the compound parts must co-occur in a document in the corpus
     * @param termCorpus The term corpus to use for lookups
     * @param weightDfObservation The weight of the observed document frequencies when combining with the score of the morphological compound pattern.
     */
    public Collector(final int minSuggestionFrequency, final int maxDecompoundExpansions, final int maxEvaluations,
                     final boolean verifyCollation, final TermCorpus termCorpus,
                     final float weightDfObservation) {

        final int queueInitialCapacity = Math.min(maxDecompoundExpansions, 10);
        collection = new PriorityQueue<>(queueInitialCapacity);

        this.minSuggestionFrequency = minSuggestionFrequency;
        this.maxDecompoundExpansions = maxDecompoundExpansions;
        this.verifyCollation = verifyCollation;
        this.termCorpus = termCorpus;
        this.weightDfObservation = weightDfObservation;
        this.maxEvaluations = maxEvaluations;
        this.totalDocsNorm = 2f * (float) Math.log(1 + termCorpus.numDocs());
    }


    /**
     *
     * @param left The modifier character sequence
     * @param right The head character sequence
     * @param rightDf The document frequency of the right term in the corpus
     * @param weightMorphologicalPattern The weight of this specific morphological pattern.
     * @return The state of candidate collection
     */
    public CollectionState collect(final CharSequence left, final CharSequence right,
                                   final int rightDf, final float weightMorphologicalPattern) {

        if (maxEvaluations <= evaluations) {
            return CollectionState.MAX_EVALUATIONS_REACHED;
        }
        evaluations++;

        final int leftDf = termCorpus.docFreq(left);
        if (leftDf >= minSuggestionFrequency) {

            final float score = weightDfObservation == 0f ? weightMorphologicalPattern
                    : weightMorphologicalPattern /
                        ((float) Math.pow(totalDocsNorm - Math.log(leftDf + 1) - Math.log(rightDf + 1),
                                weightDfObservation));

            if (verifyCollation) {

                if (((collection.size() < maxDecompoundExpansions) || (score > collection.element().score))
                        && termCorpus.coExist(left, right)) {
                    collection.offer(new Suggestion(new CharSequence[]{left, right}, score));

                    if (collection.size() > maxDecompoundExpansions) {
                        collection.poll();
                    }
                    return evaluations == maxEvaluations
                            ? CollectionState.MATCHED_MAX_EVALUATIONS_REACHED
                            : CollectionState.MATCHED_MAX_EVALUATIONS_NOT_REACHED;
                }

            } else {

                collection.offer(new Suggestion(new CharSequence[]{left, right}, score));
                if (collection.size() > maxDecompoundExpansions) {
                    collection.poll();
                }
                return evaluations == maxEvaluations
                        ? CollectionState.MATCHED_MAX_EVALUATIONS_REACHED
                        : CollectionState.MATCHED_MAX_EVALUATIONS_NOT_REACHED;
            }

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

}
