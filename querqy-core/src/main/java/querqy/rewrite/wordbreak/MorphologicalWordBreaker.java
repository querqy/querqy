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

import querqy.LowerCaseCharSequence;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class MorphologicalWordBreaker implements WordBreaker {

    public static final float DEFAULT_WEIGHT_MORPHOLOGICAL_PATTERN = 0.8f;
    private final int minBreakLength;
    private final int maxEvaluations;
    private final boolean lowerCaseInput;
    private final int minSuggestionFrequency;
    final float weightDfObservation;
    private final Morphology morphology;

    public MorphologicalWordBreaker(final Morphology morphology,
                                    final boolean lowerCaseInput, final int minSuggestionFrequency,
                                    final int minBreakLength, final int maxEvaluations) {
        this(morphology, lowerCaseInput, minSuggestionFrequency, minBreakLength, maxEvaluations,
                DEFAULT_WEIGHT_MORPHOLOGICAL_PATTERN);
    }

    public MorphologicalWordBreaker(final Morphology morphology,
                                    final boolean lowerCaseInput, final int minSuggestionFrequency,
                                    final int minBreakLength, final int maxEvaluations,
                                    final float weightMorphologicalPattern) {

        this.minBreakLength = minBreakLength;
        this.maxEvaluations = maxEvaluations;
        this.lowerCaseInput = lowerCaseInput;
        this.minSuggestionFrequency = minSuggestionFrequency;

        weightDfObservation = 1f - weightMorphologicalPattern;

        this.morphology = morphology;

    }

    @Override
    public List<CharSequence[]> breakWord(final CharSequence word,
                                          final TermCorpus termCorpus,
                                          final int maxDecompoundExpansions,
                                          final boolean verifyCollation) throws IOException {

        if (maxDecompoundExpansions < 1) {
            return Collections.emptyList();
        }

        final Collector collector = new Collector(minSuggestionFrequency, maxDecompoundExpansions, maxEvaluations,
                verifyCollation, termCorpus, weightDfObservation);

        collectSuggestions(word, termCorpus, collector);

        return collector.flushResults();

    }


    private void collectSuggestions(final CharSequence word, final TermCorpus termCorpus,
                                    final Collector collector) {
        final int termLength = Character.codePointCount(word, 0, word.length());
        if (termLength < minBreakLength) {
            return;
        }

        final CharSequence input = lowerCaseInput && (!(word instanceof LowerCaseCharSequence))
                ? new LowerCaseCharSequence(word) : word;

        // the original left term can be longer than rightOfs because the compounding might have removed characters
        // TODO: find min left size (based on linking morphemes and minBreakLength)
        // Generation of suggestions happens here -
        final List<WordBreak> suggestedWordBreaks = morphology.suggestWordBreaks(input, minBreakLength);

        for (final WordBreak suggestedWordBreak : suggestedWordBreaks) {
            final int rightDf = termCorpus.docFreq(suggestedWordBreak.originalRight);

            if (rightDf < minSuggestionFrequency) {
                continue;
            }

            for (final Suggestion suggestion : suggestedWordBreak.suggestions) {
                final Collector.CollectionState collectionState = collector.collect(
                        suggestion.sequence[0],
                        suggestedWordBreak.originalRight,
                        rightDf,
                        suggestion.score);
                if (collectionState.isMaxEvaluationsReached()) {
                    break;
                }
            }
        }
    }


}
