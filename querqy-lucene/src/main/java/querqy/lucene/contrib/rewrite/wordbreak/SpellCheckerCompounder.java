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
package querqy.lucene.contrib.rewrite.wordbreak;

import static querqy.lucene.LuceneQueryUtil.toLuceneTerm;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.spell.CombineSuggestion;
import org.apache.lucene.search.spell.SuggestMode;
import org.apache.lucene.search.spell.WordBreakSpellChecker;
import querqy.lucene.contrib.rewrite.wordbreak.LuceneCompounder;
import querqy.model.Term;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SpellCheckerCompounder implements LuceneCompounder {

    private final WordBreakSpellChecker spellChecker;
    private final String dictionaryField;
    private final boolean lowerCaseInput;

    public SpellCheckerCompounder(final WordBreakSpellChecker spellChecker, final String dictionaryField,
                                  final boolean lowerCaseInput) {
        this.spellChecker = spellChecker;
        this.dictionaryField = dictionaryField;
        this.lowerCaseInput = lowerCaseInput;
    }

    @Override
    public List<CompoundTerm> combine(final Term[] terms, final IndexReader indexReader, final boolean reverse)
            throws IOException {

        if (terms.length < 2) {
            return Collections.emptyList();
        }

        final org.apache.lucene.index.Term[] luceneTerms = new org.apache.lucene.index.Term[terms.length];

        for (int ofs = 0; ofs < terms.length; ofs++) {
            final int source = reverse ? terms.length - ofs - 1 : ofs;
            luceneTerms[ofs] = toLuceneTerm(dictionaryField, terms[source], lowerCaseInput);
        }

        final CombineSuggestion[] combineSuggestions = spellChecker
                .suggestWordCombinations(luceneTerms, 10, indexReader, SuggestMode.SUGGEST_ALWAYS);

        if (combineSuggestions == null || combineSuggestions.length < 1) {
            return Collections.emptyList();
        }

        return Arrays.stream(combineSuggestions)
                .map(combineSuggestion -> {

                    final Term[] originalTerms =
                            ((!reverse) && (combineSuggestion.originalTermIndexes().length == luceneTerms.length))
                                    ? terms
                                    : Arrays.stream(combineSuggestion.originalTermIndexes())
                                        .mapToObj(index -> reverse ? terms[terms.length - index - 1] : terms[index])
                                        .toArray(Term[]::new);
                    return new CompoundTerm(combineSuggestion.suggestion().string, originalTerms);

                })
                .collect(Collectors.toList());

    }


}
