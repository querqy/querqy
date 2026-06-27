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
package querqy.rewrite.contrib.wordbreak;

import querqy.model.Term;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MorphologicalCompounder implements Compounder {

    private static final int DEFAULT_MAX_COMPOUND_EXPANSIONS = 10;
    private final boolean lowerCaseInput;
    private final int minSuggestionFrequency;
    private final Morphology morphology;
    private final int maxCompoundExpansions;


    public MorphologicalCompounder(final Morphology morphology,
                                   final boolean lowerCaseInput,
                                   final int minSuggestionFrequency,
                                   final int maxCompoundExpansions) {
        this.lowerCaseInput = lowerCaseInput;
        this.minSuggestionFrequency = minSuggestionFrequency;
        this.morphology = morphology;
        this.maxCompoundExpansions = maxCompoundExpansions;
    }

    public MorphologicalCompounder(final Morphology morphology,
                                   final boolean lowercaseInput,
                                   final int minSuggestionFrequency) {
        this(morphology, lowercaseInput, minSuggestionFrequency, DEFAULT_MAX_COMPOUND_EXPANSIONS);
    }


    @Override
    public List<CompoundTerm> combine(final Term[] terms, final TermCorpus termCorpus, final boolean reverse) throws IOException {
        if (terms.length < 2) {
            return Collections.emptyList();
        }
        final int leftIdx = reverse ? 1 : 0;
        final int rightIdx = reverse ? 0 : 1;
        final Term left = lowerCaseInput ? terms[leftIdx].toLowerCaseTerm() : terms[leftIdx];
        final Term right = lowerCaseInput ? terms[rightIdx].toLowerCaseTerm() : terms[rightIdx];

        final int queueInitialCapacity = Math.min(maxCompoundExpansions, 10);
        final Collection<Compound> collector = Arrays.stream(morphology.suggestCompounds(left, right))
                .collect(Collectors.toCollection(() ->
                        new PriorityQueue<>(queueInitialCapacity))
                );

        return collector.stream()
                .sorted(Comparator.reverseOrder())
                .limit(maxCompoundExpansions)
                .map(compound -> new CompoundTerm(compound.compound, terms))
                .filter(compound -> termCorpus.docFreq(compound.value) >= minSuggestionFrequency)
                .collect(Collectors.toList());
    }
}
