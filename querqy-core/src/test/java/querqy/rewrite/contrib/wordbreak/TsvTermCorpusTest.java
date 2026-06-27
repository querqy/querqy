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
package querqy.rewrite.contrib.wordbreak;

import org.junit.Test;
import querqy.BloomFilter;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TsvTermCorpusTest {

    private static final int HASH_FUNCTIONS = 3;
    private static final int BLOOM_BITS = 64;

    /**
     * Builds a TSV line: term TAB df TAB bloomHex, where the bloom filter contains the given co-occurring terms.
     */
    private static String tsvLine(final String term, final int df, final String... cooccurring) {
        final BloomFilter bf = new BloomFilter(BLOOM_BITS, HASH_FUNCTIONS);
        for (final String cooc : cooccurring) {
            bf.add(cooc);
        }
        return term + "\t" + df + "\t" + bf.toHex();
    }

    private static TsvTermCorpus load(final String tsv) throws IOException {
        return TsvTermCorpus.builder()
                .reader(new StringReader(tsv))
                .hashFunctions(HASH_FUNCTIONS)
                .build();
    }

    private static TsvTermCorpus load(final String tsv, final int numDocs) throws IOException {
        return TsvTermCorpus.builder()
                .reader(new StringReader(tsv))
                .hashFunctions(HASH_FUNCTIONS)
                .numDocs(numDocs)
                .build();
    }

    @Test
    public void existsReturnsTrueForKnownTerm() throws IOException {
        final TsvTermCorpus corpus = load(tsvLine("shoe", 10, "rack"));
        assertTrue(corpus.exists("shoe"));
    }

    @Test
    public void existsReturnsFalseForUnknownTerm() throws IOException {
        final TsvTermCorpus corpus = load(tsvLine("shoe", 10, "rack"));
        assertFalse(corpus.exists("rack"));
    }

    @Test
    public void docFreqReturnsCorrectValue() throws IOException {
        final TsvTermCorpus corpus = load(tsvLine("shoe", 42, "rack"));
        assertEquals(42, corpus.docFreq("shoe"));
    }

    @Test
    public void docFreqReturnsZeroForUnknownTerm() throws IOException {
        final TsvTermCorpus corpus = load(tsvLine("shoe", 42, "rack"));
        assertEquals(0, corpus.docFreq("rack"));
    }

    @Test
    public void numDocsUsesExplicitValueWhenSet() throws IOException {
        final TsvTermCorpus corpus = load(tsvLine("shoe", 10, "rack"), 5000);
        assertEquals(5000, corpus.numDocs());
    }

    @Test
    public void numDocsIsEstimatedAsTermCountTimes100WhenNotSet() throws IOException {
        final String tsv = tsvLine("shoe", 10, "rack") + "\n"
                + tsvLine("rack", 8, "shoe") + "\n"
                + tsvLine("shelf", 5);
        final TsvTermCorpus corpus = load(tsv);
        assertEquals(300, corpus.numDocs());
    }

    @Test
    public void coExistReturnsTrueWhenTerm2IsInBloomFilterOfTerm1() throws IOException {
        final TsvTermCorpus corpus = load(tsvLine("shoe", 10, "rack"));
        assertTrue(corpus.coExist("shoe", "rack"));
    }

    @Test
    public void coExistReturnsFalseWhenTerm2IsNotInBloomFilterOfTerm1() throws IOException {
        final TsvTermCorpus corpus = load(tsvLine("shoe", 10, "rack"));
        assertFalse(corpus.coExist("shoe", "shelf"));
    }

    @Test
    public void coExistReturnsFalseForUnknownTerm1() throws IOException {
        final TsvTermCorpus corpus = load(tsvLine("shoe", 10, "rack"));
        assertFalse(corpus.coExist("rack", "shoe"));
    }

    @Test
    public void emptyLinesAreSkipped() throws IOException {
        final String tsv = "\n" + tsvLine("shoe", 10, "rack") + "\n\n";
        final TsvTermCorpus corpus = load(tsv);
        assertTrue(corpus.exists("shoe"));
        assertEquals(100, corpus.numDocs());
    }

    @Test
    public void whitespaceAroundFieldsIsTrimmed() throws IOException {
        final BloomFilter bf = new BloomFilter(BLOOM_BITS, HASH_FUNCTIONS);
        bf.add("rack");
        final String tsv = " shoe \t 7 \t " + bf.toHex() + " ";
        final TsvTermCorpus corpus = load(tsv);
        assertTrue(corpus.exists("shoe"));
        assertEquals(7, corpus.docFreq("shoe"));
        assertTrue(corpus.coExist("shoe", "rack"));
    }

    @Test
    public void multipleTermsAreAllLoaded() throws IOException {
        final String tsv = tsvLine("shoe", 10, "rack") + "\n"
                + tsvLine("rack", 8, "shoe") + "\n"
                + tsvLine("shelf", 5, "rack", "shoe");
        final TsvTermCorpus corpus = load(tsv);
        assertTrue(corpus.exists("shoe"));
        assertTrue(corpus.exists("rack"));
        assertTrue(corpus.exists("shelf"));
        assertTrue(corpus.coExist("shelf", "rack"));
        assertTrue(corpus.coExist("shelf", "shoe"));
    }

    @Test(expected = IllegalStateException.class)
    public void builderThrowsWhenReaderIsMissing() throws IOException {
        TsvTermCorpus.builder().hashFunctions(HASH_FUNCTIONS).build();
    }

    @Test(expected = IllegalStateException.class)
    public void builderThrowsWhenHashFunctionsIsMissing() throws IOException {
        TsvTermCorpus.builder().reader(new StringReader("")).build();
    }

    @Test(expected = IOException.class)
    public void buildThrowsOnMalformedLine() throws IOException {
        load("schuh\t10");
    }
}
