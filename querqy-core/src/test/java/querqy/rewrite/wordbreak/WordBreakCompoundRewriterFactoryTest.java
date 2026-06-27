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

import org.junit.Test;
import querqy.model.Term;
import querqy.trie.TrieMap;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WordBreakCompoundRewriterFactoryTest {

    /** Builds a TSV line with an empty (all-zeros) bloom filter — sufficient when verifyCollation=false. */
    private static String termLine(final String term, final int df) {
        return term + "\t" + df + "\t" + "0000000000000000";
    }

    private static TsvDfCoocTermCorpus emptyCorpus() throws IOException {
        return TsvDfCoocTermCorpus.builder()
                .reader(new StringReader(""))
                .hashFunctions(1)
                .build();
    }

    private static TsvDfCoocTermCorpus corpusOf(final String... terms) throws IOException {
        final String tsv = Arrays.stream(terms)
                .map(t -> termLine(t, 1))
                .collect(Collectors.joining("\n"));
        return TsvDfCoocTermCorpus.builder()
                .reader(new StringReader(tsv))
                .hashFunctions(1)
                .build();
    }

    @Test
    public void testThatTriggerWordsAreTurnedToLowerCaseForFlagLowerCaseInputTrue() throws IOException {
        final WordBreakCompoundRewriterFactory factory = new WordBreakCompoundRewriterFactory(
                "w1", emptyCorpus(), true, 1, 1,
                Arrays.asList("Word1", "word2"), false, 2, false,
                Collections.emptyList(), "DEFAULT", "DEFAULT");

        final TrieMap<Boolean> triggerWords = factory.getReverseCompoundTriggerWords();
        assertTrue(triggerWords.get("word1").getStateForCompleteSequence().isFinal());
        assertTrue(triggerWords.get("word2").getStateForCompleteSequence().isFinal());
    }

    @Test
    public void testThatTriggerWordsAreTurnedToLowerCaseForFlagLowerCaseInputFalse() throws IOException {
        final WordBreakCompoundRewriterFactory factory = new WordBreakCompoundRewriterFactory(
                "w2", emptyCorpus(), false, 1, 1,
                Arrays.asList("Word1", "word2"), false, 2, false,
                Collections.emptyList(), "DEFAULT", "DEFAULT");

        final TrieMap<Boolean> triggerWords = factory.getReverseCompoundTriggerWords();
        assertFalse(triggerWords.get("word1").getStateForCompleteSequence().isFinal());
        assertTrue(triggerWords.get("Word1").getStateForCompleteSequence().isFinal());
        assertTrue(triggerWords.get("word2").getStateForCompleteSequence().isFinal());
    }

    @Test
    public void testThatProtectedWordsAreMatchedCaseInsensitiveForFlagLowerCaseInputTrue() throws IOException {
        final WordBreakCompoundRewriterFactory factory = new WordBreakCompoundRewriterFactory(
                "w1", emptyCorpus(), true, 1, 1,
                Arrays.asList("Word1", "word2"), false, 2, false,
                Collections.singletonList("Protected"), "DEFAULT", "DEFAULT");

        final TrieMap<Boolean> protectedWords = factory.getProtectedWords();
        assertTrue(protectedWords.get("protected").getStateForCompleteSequence().isFinal());
    }

    @Test
    public void testThatProtectedWordsAreMatchedCaseSensitiveForFlagLowerCaseInputFalse() throws IOException {
        final WordBreakCompoundRewriterFactory factory = new WordBreakCompoundRewriterFactory(
                "w2", emptyCorpus(), false, 1, 1,
                Arrays.asList("Word1", "word2"), false, 2, false,
                Collections.singletonList("Protected"), "GERMAN", "DEFAULT");

        final TrieMap<Boolean> protectedWords = factory.getProtectedWords();
        assertTrue(protectedWords.get("Protected").getStateForCompleteSequence().isFinal());
        assertFalse(protectedWords.get("protected").getStateForCompleteSequence().isFinal());
    }

    @Test
    public void testLanguageDecompoundingMorphologyIsApplied() throws Exception {
        final TsvDfCoocTermCorpus corpus = corpusOf("regal", "buch", "büch", "bücher", "büchere");

        final WordBreakCompoundRewriterFactory factory = new WordBreakCompoundRewriterFactory(
                "w2", emptyCorpus(), false, 1, 1,
                Arrays.asList("Word1", "word2"), false, 2, false,
                Collections.emptyList(), "GERMAN", "DEFAULT");

        assertTrue(factory.getWordBreaker() instanceof MorphologicalWordBreaker);

        assertThat(
                factory.getWordBreaker().breakWord("bücherregal", corpus, 10, false).stream()
                        .map(charSequences -> charSequences[0])
                        .map(CharSequence::toString)
                        .collect(Collectors.toList()),
                containsInAnyOrder("buch", "büch", "bücher", "büchere"));
    }

    @Test
    public void testDefaultLanguageCompoundingMorphologyIsApplied() throws Exception {
        final TsvDfCoocTermCorpus corpus = corpusOf("buchregal");

        final WordBreakCompoundRewriterFactory factory = new WordBreakCompoundRewriterFactory(
                "w2", emptyCorpus(), false, 1, 1,
                Arrays.asList("Word1", "word2"), false, 2, false,
                Collections.emptyList(), "GERMAN", "DEFAULT");

        assertTrue(factory.getCompounder() instanceof MorphologicalCompounder);

        final Term leftTerm  = new Term(null, "field1", "buch");
        final Term rightTerm = new Term(null, "field1", "regal");
        assertThat(
                factory.getCompounder().combine(new Term[]{leftTerm, rightTerm}, corpus, false).stream()
                        .map(compoundTerm -> compoundTerm.value)
                        .map(CharSequence::toString)
                        .collect(Collectors.toList()),
                containsInAnyOrder("buchregal"));
    }

    @Test
    public void testLanguageCompoundingMorphologyIsApplied() throws Exception {
        final TsvDfCoocTermCorpus corpus = corpusOf("bücherregal");

        final WordBreakCompoundRewriterFactory factory = new WordBreakCompoundRewriterFactory(
                "w2", emptyCorpus(), false, 1, 1,
                Arrays.asList("Word1", "word2"), false, 2, false,
                Collections.emptyList(), "DEFAULT", "GERMAN");

        assertTrue(factory.getCompounder() instanceof MorphologicalCompounder);

        final Term leftTerm  = new Term(null, "field1", "büch");
        final Term rightTerm = new Term(null, "field1", "regal");
        assertThat(
                factory.getCompounder().combine(new Term[]{leftTerm, rightTerm}, corpus, false).stream()
                        .map(compoundTerm -> compoundTerm.value)
                        .map(CharSequence::toString)
                        .collect(Collectors.toList()),
                containsInAnyOrder("bücherregal"));
    }

    @Test
    public void testThatFactoryThrowsWhenVerifyCollationRequiredButCorpusDoesNotSupportIt() throws IOException {
        final TsvDfTermCorpus corpusWithoutCooc = TsvDfTermCorpus.builder()
                .reader(new StringReader(""))
                .build();
        try {
            new WordBreakCompoundRewriterFactory(
                    "w1", corpusWithoutCooc, true, 1, 1,
                    Collections.emptyList(), false, 2, true,
                    Collections.emptyList(), "DEFAULT", "DEFAULT");
            fail("expected IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            // expected
        }
    }
}
