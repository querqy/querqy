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
package querqy.rewrite.wordbreak;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TsvDfTermCorpusTest {

    private static TsvDfTermCorpus load(final String tsv) throws IOException {
        return TsvDfTermCorpus.builder()
                .reader(new StringReader(tsv))
                .build();
    }

    private static TsvDfTermCorpus load(final String tsv, final int numDocs) throws IOException {
        return TsvDfTermCorpus.builder()
                .reader(new StringReader(tsv))
                .numDocs(numDocs)
                .build();
    }

    @Test
    public void existsReturnsTrueForKnownTerm() throws IOException {
        assertTrue(load("shoe\t10").exists("shoe"));
    }

    @Test
    public void existsReturnsFalseForUnknownTerm() throws IOException {
        assertFalse(load("shoe\t10").exists("rack"));
    }

    @Test
    public void docFreqReturnsCorrectValue() throws IOException {
        assertEquals(42, load("shoe\t42").docFreq("shoe"));
    }

    @Test
    public void docFreqReturnsZeroForUnknownTerm() throws IOException {
        assertEquals(0, load("shoe\t42").docFreq("rack"));
    }

    @Test
    public void numDocsUsesExplicitValueWhenSet() throws IOException {
        assertEquals(5000, load("shoe\t10", 5000).numDocs());
    }

    @Test
    public void numDocsIsEstimatedAsTermCountTimes100WhenNotSet() throws IOException {
        final String tsv = "shoe\t10\nrack\t8\nshelf\t5";
        assertEquals(300, load(tsv).numDocs());
    }

    @Test
    public void emptyLinesAreSkipped() throws IOException {
        final TsvDfTermCorpus corpus = load("\nshoe\t10\n\n");
        assertTrue(corpus.exists("shoe"));
        assertEquals(100, corpus.numDocs());
    }

    @Test
    public void whitespaceAroundFieldsIsTrimmed() throws IOException {
        final TsvDfTermCorpus corpus = load(" shoe \t 7 ");
        assertTrue(corpus.exists("shoe"));
        assertEquals(7, corpus.docFreq("shoe"));
    }

    @Test
    public void multipleTermsAreAllLoaded() throws IOException {
        final TsvDfTermCorpus corpus = load("shoe\t10\nrack\t8\nshelf\t5");
        assertTrue(corpus.exists("shoe"));
        assertTrue(corpus.exists("rack"));
        assertTrue(corpus.exists("shelf"));
    }

    @Test
    public void isCollationSupportedReturnsFalse() throws IOException {
        assertFalse(load("shoe\t10").isCollationSupported());
    }

    @Test
    public void coExistThrowsUnsupportedOperationException() throws IOException {
        final TsvDfTermCorpus corpus = load("shoe\t10\nrack\t8");
        try {
            corpus.coExist("shoe", "rack");
            fail("expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    @Test(expected = IllegalStateException.class)
    public void builderThrowsWhenReaderIsMissing() throws IOException {
        TsvDfTermCorpus.builder().build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderThrowsOnZeroNumDocs() {
        TsvDfTermCorpus.builder().numDocs(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderThrowsOnNegativeNumDocs() {
        TsvDfTermCorpus.builder().numDocs(-1);
    }

    @Test(expected = IOException.class)
    public void buildThrowsOnMalformedLine() throws IOException {
        load("shoe");
    }

    @Test(expected = IOException.class)
    public void buildThrowsOnZeroDocFreq() throws IOException {
        load("shoe\t0");
    }

    @Test(expected = IOException.class)
    public void buildThrowsOnNegativeDocFreq() throws IOException {
        load("shoe\t-1");
    }
}
