/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2022 Querqy Contributors
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
package querqy.rewriter.wordbreak;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;


public class SuffixGroupMorphology implements Morphology {

    private final Function<Float, SuffixGroup> morphemeFactory;

    private final Function<Float, SuffixGroup> compoundingMorphemeFactory;


    public SuffixGroupMorphology(final Function<Float, SuffixGroup> wordBreakMorphemeFactory,
                          final Function<Float, SuffixGroup> compoundingMorphemeFactory) {
        this.morphemeFactory = wordBreakMorphemeFactory;
        this.compoundingMorphemeFactory = compoundingMorphemeFactory;
    }

    public SuffixGroupMorphology(final Function<Float, SuffixGroup> morphemeFactory) {
        this(morphemeFactory, morphemeFactory);
    }

    private SuffixGroup createMorphemes() {
        return morphemeFactory.apply(MorphologicalWordBreaker.DEFAULT_WEIGHT_MORPHOLOGICAL_PATTERN);
    }

    @Override
    public Compound[] suggestCompounds(final CharSequence left, final CharSequence right) {
        final SuffixGroup morphemes = compoundingMorphemeFactory.apply(MorphologicalWordBreaker.DEFAULT_WEIGHT_MORPHOLOGICAL_PATTERN);

        return morphemes.generateCompoundSuggestions(left, right)
                .stream().distinct()
                .map(suggestion -> new Compound(new CharSequence[]{left, right},
                        suggestion.sequence[0],
                        suggestion.score)).toArray(Compound[]::new);
    }

    @Override
    public List<WordBreak> suggestWordBreaks(final CharSequence word, final int minBreakLength) {
        final SuffixGroup morphemes = createMorphemes();
        final int termLength = Character.codePointCount(word, 0, word.length());
        final List<WordBreak> wordBreaks = new ArrayList<>();
        for (int leftLength = termLength - minBreakLength; leftLength > 0; leftLength--) {
            if (leftLength < minBreakLength || (termLength - leftLength) < minBreakLength) {
                //skip if right or left term is smaller than minBreakLength
                continue;
            }
            final int splitIndex = Character.offsetByCodePoints(word, 0, leftLength);
            final CharSequence right = word.subSequence(splitIndex, word.length());
            final CharSequence left = word.subSequence(0, splitIndex);
            final List<Suggestion> suggestions = morphemes.generateSuggestions(left).stream()
                    .filter(breakSuggestion -> breakSuggestion.sequence[0].length() >= minBreakLength)
                    .distinct()
                    .collect(Collectors.toList());
            wordBreaks.add(new WordBreak(left, right, suggestions));
        }

        return wordBreaks;
    }


}
