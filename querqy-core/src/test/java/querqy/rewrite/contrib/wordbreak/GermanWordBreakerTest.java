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
package querqy.rewrite.contrib.wordbreak;

import org.junit.Test;
import querqy.BloomFilter;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GermanWordBreakerTest {

    private static final int HASH_FUNCTIONS = 7;
    private static final int BLOOM_BITS = 128;

    private final Morphology GERMAN = new MorphologyProvider().get("GERMAN").get();

    /**
     * Accumulates term-pair entries (as used in addNumDocsWithTextField) and builds a TsvTermCorpus.
     * Each call to add(termsSpaceSep, count) contributes count documents, updating docFreq for each
     * term and recording co-occurrence between all terms in the space-separated group.
     */
    private static class TestCorpusBuilder {
        private final Map<String, Integer> df = new LinkedHashMap<>();
        private final Map<String, Set<String>> cooc = new LinkedHashMap<>();
        private int totalDocs = 0;

        TestCorpusBuilder add(final String termsSpaceSep, final int count) {
            final String[] terms = termsSpaceSep.split(" ");
            totalDocs += count;
            for (final String t : terms) {
                df.merge(t, count, Integer::sum);
            }
            for (final String t : terms) {
                for (final String other : terms) {
                    if (!t.equals(other)) {
                        cooc.computeIfAbsent(t, k -> new HashSet<>()).add(other);
                    }
                }
            }
            return this;
        }

        TsvTermCorpus build() throws IOException {
            final StringBuilder sb = new StringBuilder();
            for (final Map.Entry<String, Integer> entry : df.entrySet()) {
                final String term = entry.getKey();
                final int freq = entry.getValue();
                final BloomFilter bf = new BloomFilter(BLOOM_BITS, HASH_FUNCTIONS);
                for (final String coocTerm : cooc.getOrDefault(term, Collections.emptySet())) {
                    bf.add(coocTerm);
                }
                sb.append(term).append('\t').append(freq).append('\t').append(bf.toHex()).append('\n');
            }
            return TsvTermCorpus.builder()
                    .reader(new StringReader(sb.toString()))
                    .hashFunctions(HASH_FUNCTIONS)
                    .numDocs(totalDocs)
                    .build();
        }
    }

    private static TsvTermCorpus emptyCorpus() throws IOException {
        return TsvTermCorpus.builder()
                .reader(new StringReader(""))
                .hashFunctions(HASH_FUNCTIONS)
                .numDocs(0)
                .build();
    }

    @Test
    public void testWithEmptyIndex() throws IOException {
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 2, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("abcdef", emptyCorpus(), 2, true);
        assertNotNull(sequences);
        assertTrue(sequences.isEmpty());
    }

    @Test
    public void testWithNoExistentDictField() throws IOException {
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 2, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("abcdef", emptyCorpus(), 2, true);
        assertNotNull(sequences);
        assertTrue(sequences.isEmpty());
    }

    @Test
    public void testNoLinkingMorpheme() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder()
                .add("abc def", 4)
                .add("ab cdef", 10)
                .add("abcd ef", 5)
                .build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 2, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("abcdef", corpus, 2, true);
        assertThat(sequences, contains(
                equalTo(new CharSequence[]{"ab", "cdef"}),
                equalTo(new CharSequence[]{"abcd", "ef"})));
    }

    @Test
    public void testSplitAtLinkingMorphemeE() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder().add("hund futter", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 2, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("hundefutter", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"hund", "futter"})));
    }

    @Test
    public void testSplitAtLinkingMorphemeN() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder().add("matte laden", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 2, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("mattenladen", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"matte", "laden"})));
    }

    @Test
    public void testSplitAtLinkingMorphemeS() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder().add("arbeit matten", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 2, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("arbeitsmatten", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"arbeit", "matten"})));
    }

    @Test
    public void testSingleLetterCandidateS() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder().add("s chiller", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 1, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("schiller", corpus, 2, true);
        // We don't care which strategy produces this but let's make sure, we don't crash.
        assertThat(sequences, contains(equalTo(new CharSequence[]{"s", "chiller"})));
    }

    @Test
    public void testNoLinkingMorphemeIsPreferredOverMorphemeSForSameCollationFrequency() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder()
                .add("fan shirt", 20)
                .add("fan hirt", 20)
                .build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 2, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("fanshirt", corpus, 2, true);
        assertThat(sequences, contains(
                equalTo(new CharSequence[]{"fan", "shirt"}),
                equalTo(new CharSequence[]{"fan", "hirt"})));
    }

    @Test
    public void testThatHighDfObservatioWeightWeighsMoreThanMorphoSyntaxPrior() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder()
                .add("fan shirt", 1)
                .add("fan hirt", 10)
                .build();

        // use higher weight for morph-syntax structure first
        assertThat(
                new MorphologicalWordBreaker(GERMAN, true, 1, 2, 100, 0.8f)
                        .breakWord("fanshirt", corpus, 1, true),
                contains(equalTo(new CharSequence[]{"fan", "shirt"})));

        // use low weight for morph-syntax structure first
        assertThat(
                new MorphologicalWordBreaker(GERMAN, true, 1, 2, 100, 0.1f)
                        .breakWord("fanshirt", corpus, 1, true),
                contains(equalTo(new CharSequence[]{"fan", "hirt"})));
    }

    @Test
    public void testMinBreakSizeAtLinkingMorphemeS() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder().add("arbeit verträge", 1).build();
        // minBreakLength must relate to prefix word w/o linking morpheme
        assertTrue(
                new MorphologicalWordBreaker(GERMAN, true, 1, 7, 100)
                        .breakWord("arbeitsverträge", corpus, 2, true).isEmpty());
        assertFalse(
                new MorphologicalWordBreaker(GERMAN, true, 1, 6, 100)
                        .breakWord("arbeitsverträge", corpus, 2, true).isEmpty());
    }

    @Test
    public void testSplitAtLinkingMorphemeEn() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder().add("strauß ei", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 2, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("straußenei", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"strauß", "ei"})));
    }

    @Test
    public void testSplitAtLinkingMorphemeNen() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder().add("wöchnerin heim", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 2, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("wöchnerinnenheim", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"wöchnerin", "heim"})));
    }

    @Test
    public void testSplitAtLinkingMorphemeIen() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder().add("prinzip reiter", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 2, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("prinzipienreiter", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"prinzip", "reiter"})));
    }

    @Test
    public void testSplitAtLinkingMorphemeEs() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder().add("tag zeit", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 3, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("tageszeit", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"tag", "zeit"})));
    }

    @Test
    public void testMinBreakSizeAtLinkingMorphemeEs() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder().add("tag zeit", 1).build();
        // minBreakLength must relate to prefix word w/o linking morpheme
        assertTrue(
                new MorphologicalWordBreaker(GERMAN, true, 1, 4, 100)
                        .breakWord("tageszeit", corpus, 2, true).isEmpty());
        assertFalse(
                new MorphologicalWordBreaker(GERMAN, true, 1, 3, 100)
                        .breakWord("tageszeit", corpus, 2, true).isEmpty());
    }

    @Test
    public void testSplitAtLinkingMorphemeEr() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder().add("geist stunde", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 3, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("geisterstunde", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"geist", "stunde"})));
    }

    @Test
    public void testSplitAtLinkingMorphemeUmlautEr() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder()
                .add("buch regal", 1)
                .add("blatt wald", 1)
                .add("korn brötchen", 1)
                .build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 3, 100);

        assertThat(wordBreaker.breakWord("bücherregal", corpus, 2, true),
                contains(equalTo(new CharSequence[]{"buch", "regal"})));
        assertThat(wordBreaker.breakWord("blätterwald", corpus, 2, true),
                contains(equalTo(new CharSequence[]{"blatt", "wald"})));
        assertThat(wordBreaker.breakWord("körnerbrötchen", corpus, 2, true),
                contains(equalTo(new CharSequence[]{"korn", "brötchen"})));
    }

    @Test
    public void testSplitAtLinkingMorphemeUmlautE() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder()
                .add("gans klein", 1)
                .add("laus kamm", 1)
                .add("korb macher", 1)
                .build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 3, 100);

        assertThat(wordBreaker.breakWord("gänseklein", corpus, 2, true),
                contains(equalTo(new CharSequence[]{"gans", "klein"})));
        assertThat(wordBreaker.breakWord("läusekamm", corpus, 2, true),
                contains(equalTo(new CharSequence[]{"laus", "kamm"})));
        assertThat(wordBreaker.breakWord("körbemacher", corpus, 2, true),
                contains(equalTo(new CharSequence[]{"korb", "macher"})));
    }

    @Test
    public void testSplitAtLinkingMorphemeEnRemovingUs() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder().add("aphorismus sammlung", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 3, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("aphorismensammlung", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"aphorismus", "sammlung"})));
    }

    @Test
    public void testSplitAtLinkingMorphemeEnRemovingUm() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder().add("museum verwaltung", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 3, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("museenverwaltung", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"museum", "verwaltung"})));
    }

    @Test
    public void testSplitAtLinkingMorphemeEnRemovingA() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder().add("madonna kult", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 3, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("madonnenkult", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"madonna", "kult"})));
    }

    @Test
    public void testSplitAtLinkingMorphemeEnRemovingOnPlusEn() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder().add("stadion verbot", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 3, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("stadienverbot", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"stadion", "verbot"})));
    }

    @Test
    public void testSplitAtLinkingMorphemeRemovingOnPlusA() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder().add("pharmakon analyse", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 3, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("pharmakaanalyse", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"pharmakon", "analyse"})));
    }

    @Test
    public void testSplitAtLinkingMorphemeRemovingEPlusI() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder().add("carabiniere schule", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 3, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("carabinierischule", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"carabiniere", "schule"})));
    }

    @Test
    public void testSplitRemovingE() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder().add("baumwolle tuch", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 3, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("baumwolltuch", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"baumwolle", "tuch"})));
    }

    @Test
    public void testSplitRemovingEn() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder().add("süden wind", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 3, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("südwind", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"süden", "wind"})));
    }

    @Test
    public void testSplitAtLinkingMorphemeARemovingUm() throws IOException {
        final TsvTermCorpus corpus = new TestCorpusBuilder().add("aphrodisiakum verkäufer", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, true, 1, 3, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("aphrodisiakaverkäufer", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"aphrodisiakum", "verkäufer"})));
    }
}
