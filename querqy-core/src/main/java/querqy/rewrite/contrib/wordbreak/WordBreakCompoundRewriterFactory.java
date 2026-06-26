/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018 Querqy Contributors
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

import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.trie.TrieMap;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class WordBreakCompoundRewriterFactory extends RewriterFactory {

    private static final int MAX_EVALUATIONS = 100;

    private final boolean lowerCaseInput;
    private final boolean alwaysAddReverseCompounds;
    private final TrieMap<Boolean> reverseCompoundTriggerWords;
    private final int maxDecompoundExpansions;
    private final boolean verifyDecompoundCollation;
    private final WordBreaker wordBreaker;
    private final Compounder compounder;
    private final TrieMap<Boolean> protectedWords;
    private final TermCorpus termCorpus;

    /**
     * @param rewriterId                  The id of the rewriter
     * @param termCorpus                  The term corpus for dictionary lookups
     * @param lowerCaseInput              Iff true, lowercase input before matching it against the dictionary field.
     * @param minSuggestionFreq           The minimum frequency of a suggestion in the dictionary field
     * @param minBreakLength              The minimum word part length for decompounding
     * @param reverseCompoundTriggerWords Query tokens in this list will trigger the creation of a reverse compound of the surrounding tokens.
     * @param alwaysAddReverseCompounds   Iff true, reverse shingles will be added to the query
     * @param maxDecompoundExpansions     The maximum number of decompounds to add to the query
     * @param verifyDecompoundCollation   Iff true, verify that all parts of the compound cooccur in dictionaryField after decompounding
     * @param protectedWords              Do not split these words
     * @param decompoundMorphologyName    The name of decompounding morphology to use
     * @param compoundMorphologyName      The name of compounding morphology to use
     */
    public WordBreakCompoundRewriterFactory(final String rewriterId,
                                            final TermCorpus termCorpus,
                                            final boolean lowerCaseInput,
                                            final int minSuggestionFreq,
                                            final int minBreakLength,
                                            final List<String> reverseCompoundTriggerWords,
                                            final boolean alwaysAddReverseCompounds,
                                            final int maxDecompoundExpansions,
                                            final boolean verifyDecompoundCollation,
                                            final List<String> protectedWords,
                                            final String decompoundMorphologyName,
                                            final String compoundMorphologyName) {
        super(rewriterId);
        this.termCorpus = termCorpus;
        this.lowerCaseInput = lowerCaseInput;
        this.alwaysAddReverseCompounds = alwaysAddReverseCompounds;
        this.verifyDecompoundCollation = verifyDecompoundCollation;
        if (maxDecompoundExpansions < 0) {
            throw new IllegalArgumentException("maxDecompoundExpansions >= 0 required. Actual value: "
                    + maxDecompoundExpansions);
        }
        this.maxDecompoundExpansions = maxDecompoundExpansions;

        this.reverseCompoundTriggerWords = buildWordLookup(reverseCompoundTriggerWords, lowerCaseInput);
        this.protectedWords = buildWordLookup(protectedWords, lowerCaseInput);

        final MorphologyProvider morphologyProvider = new MorphologyProvider();
        final Optional<Morphology> compoundMorphology = morphologyProvider.get(compoundMorphologyName);
        final Optional<Morphology> decompoundMorphology = morphologyProvider.get(decompoundMorphologyName);

        compounder = new MorphologicalCompounder(
                compoundMorphology.orElse(MorphologyProvider.DEFAULT),
                lowerCaseInput, minSuggestionFreq);

        wordBreaker = new MorphologicalWordBreaker(
                decompoundMorphology.orElse(MorphologyProvider.DEFAULT),
                lowerCaseInput, minSuggestionFreq, minBreakLength, MAX_EVALUATIONS);
    }

    @Override
    public QueryRewriter createRewriter(final ExpandedQuery input,
                                        final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return new WordBreakCompoundRewriter(wordBreaker, compounder, termCorpus,
                lowerCaseInput, alwaysAddReverseCompounds, reverseCompoundTriggerWords, maxDecompoundExpansions,
                verifyDecompoundCollation, protectedWords);
    }

    @Override
    public Set<Term> getCacheableGenerableTerms() {
        return QueryRewriter.EMPTY_GENERABLE_TERMS;
    }

    public WordBreaker getWordBreaker() {
        return wordBreaker;
    }

    public Compounder getCompounder() {
        return compounder;
    }

    public TrieMap<Boolean> getReverseCompoundTriggerWords() {
        return reverseCompoundTriggerWords;
    }

    public TrieMap<Boolean> getProtectedWords() {
        return protectedWords;
    }

    private static TrieMap<Boolean> buildWordLookup(final Collection<String> words, final boolean lowerCase) {
        final TrieMap<Boolean> result = new TrieMap<>();
        if (words != null) {
            words.forEach(word -> result.put(lowerCase ? word.toLowerCase() : word, true));
        }
        return result;
    }
}
