/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2026 Querqy Contributors
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * <p>End-to-end decompounding tests for the {@code DUTCH} morphology: real {@link MorphologicalWordBreaker} +
 * real {@link TermCorpus} (not just candidate generation, as in {@link DutchMorphologyDecompoundingTableTest}), in
 * the style of {@link GermanWordBreakerTest}.</p>
 */
public class DutchWordBreakerTest {

    private static final int HASH_FUNCTIONS = 7;
    private static final int BLOOM_BITS = 128;

    private final Morphology DUTCH = new MorphologyProvider().get("dutch").get();

    /**
     * Accumulates term-pair entries (as used in addNumDocsWithTextField) and builds a TsvDfCoocTermCorpus.
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

        TsvDfCoocTermCorpus build() throws IOException {
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
            return TsvDfCoocTermCorpus.builder()
                    .reader(new StringReader(sb.toString()))
                    .hashFunctions(HASH_FUNCTIONS)
                    .numDocs(totalDocs)
                    .build();
        }
    }

    private static TsvDfCoocTermCorpus emptyCorpus() throws IOException {
        return TsvDfCoocTermCorpus.builder()
                .reader(new StringReader(""))
                .hashFunctions(HASH_FUNCTIONS)
                .build();
    }

    @Test
    public void testWithEmptyIndex() throws IOException {
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(DUTCH, true, 1, 2, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("abcdef", emptyCorpus(), 2, true);
        assertNotNull(sequences);
        assertTrue(sequences.isEmpty());
    }

    @Test
    public void testNoLinkingMorpheme() throws IOException {
        final TsvDfCoocTermCorpus corpus = new TestCorpusBuilder().add("post kantoor", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(DUTCH, true, 1, 2, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("postkantoor", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"post", "kantoor"})));
    }

    @Test
    public void testSplitAtLinkingMorphemeS() throws IOException {
        final TsvDfCoocTermCorpus corpus = new TestCorpusBuilder().add("bevolking groei", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(DUTCH, true, 1, 2, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("bevolkingsgroei", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"bevolking", "groei"})));
    }

    @Test
    public void testSplitAtLinkingMorphemeEnPlain() throws IOException {
        final TsvDfCoocTermCorpus corpus = new TestCorpusBuilder().add("boek kast", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(DUTCH, true, 1, 2, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("boekenkast", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"boek", "kast"})));
    }

    @Test
    public void testSplitAtLinkingMorphemeEr() throws IOException {
        final TsvDfCoocTermCorpus corpus = new TestCorpusBuilder()
                .add("kind arts", 1)
                .add("rund gehakt", 1)
                .build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(DUTCH, true, 1, 2, 100);
        assertThat(wordBreaker.breakWord("kinderarts", corpus, 2, true),
                contains(equalTo(new CharSequence[]{"kind", "arts"})));
        assertThat(wordBreaker.breakWord("rundergehakt", corpus, 2, true),
                contains(equalTo(new CharSequence[]{"rund", "gehakt"})));
    }

    @Test
    public void testSplitWithDegeminationE() throws IOException {
        final TsvDfCoocTermCorpus corpus = new TestCorpusBuilder().add("zon bril", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(DUTCH, true, 1, 2, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("zonnebril", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"zon", "bril"})));
    }

    @Test
    public void testSplitWithDegeminationEn() throws IOException {
        final TsvDfCoocTermCorpus corpus = new TestCorpusBuilder().add("pad poel", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(DUTCH, true, 1, 2, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("paddenpoel", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"pad", "poel"})));
    }

    @Test
    public void testSplitWithVowelLengtheningE() throws IOException {
        final TsvDfCoocTermCorpus corpus = new TestCorpusBuilder().add("peer boom", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(DUTCH, true, 1, 2, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("pereboom", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"peer", "boom"})));
    }

    @Test
    public void testSplitWithVowelLengtheningEn() throws IOException {
        final TsvDfCoocTermCorpus corpus = new TestCorpusBuilder().add("schaap vlees", 1).build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(DUTCH, true, 1, 2, 100);
        final List<CharSequence[]> sequences = wordBreaker.breakWord("schapenvlees", corpus, 2, true);
        assertThat(sequences, contains(equalTo(new CharSequence[]{"schaap", "vlees"})));
    }

    @Test
    public void testSplitWithDevoicingEn() throws IOException {
        final TsvDfCoocTermCorpus corpus = new TestCorpusBuilder()
                .add("duif hok", 1)
                .add("huis blok", 1)
                .build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(DUTCH, true, 1, 2, 100);
        assertThat(wordBreaker.breakWord("duivenhok", corpus, 2, true),
                contains(equalTo(new CharSequence[]{"duif", "hok"})));
        assertThat(wordBreaker.breakWord("huizenblok", corpus, 2, true),
                contains(equalTo(new CharSequence[]{"huis", "blok"})));
    }

    @Test
    public void testSplitWithLengtheningAndDevoicingEn() throws IOException {
        final TsvDfCoocTermCorpus corpus = new TestCorpusBuilder()
                .add("slaaf handel", 1)
                .add("graaf straat", 1)
                .build();
        final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(DUTCH, true, 1, 2, 100);
        assertThat(wordBreaker.breakWord("slavenhandel", corpus, 2, true),
                contains(equalTo(new CharSequence[]{"slaaf", "handel"})));
        assertThat(wordBreaker.breakWord("gravenstraat", corpus, 2, true),
                contains(equalTo(new CharSequence[]{"graaf", "straat"})));
    }

    @Test
    public void testMinBreakSizeAtLinkingMorphemeEr() throws IOException {
        final TsvDfCoocTermCorpus corpus = new TestCorpusBuilder().add("kind arts", 1).build();
        // minBreakLength must relate to prefix word w/o linking morpheme
        assertTrue(
                new MorphologicalWordBreaker(DUTCH, true, 1, 5, 100)
                        .breakWord("kinderarts", corpus, 2, true).isEmpty());
        assertThat(
                new MorphologicalWordBreaker(DUTCH, true, 1, 4, 100)
                        .breakWord("kinderarts", corpus, 2, true),
                contains(equalTo(new CharSequence[]{"kind", "arts"})));
    }
}
