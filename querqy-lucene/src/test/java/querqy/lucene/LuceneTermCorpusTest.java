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
package querqy.lucene;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.tests.index.RandomIndexWriter;
import org.apache.lucene.tests.util.LuceneTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static querqy.lucene.rewrite.TestUtil.addNumDocsWithTextField;

public class LuceneTermCorpusTest extends LuceneTestCase {

    private static Directory directory;
    private static IndexReader reader;
    private static LuceneTermCorpus corpus;

    /**
     * Shared index for all tests:
     *   field "f1": 5 docs with "apple banana", 3 docs with "cherry"  → 8 docs total
     *   field "f2": 2 docs with "apple"  — used to verify field scoping
     */
    @BeforeClass
    public static void setupIndex() throws IOException {
        directory = newDirectory();
        final RandomIndexWriter writer = new RandomIndexWriter(random(), directory, new WhitespaceAnalyzer());
        addNumDocsWithTextField("f1", "apple banana", writer, 5);
        addNumDocsWithTextField("f1", "cherry", writer, 3);
        addNumDocsWithTextField("f2", "apple", writer, 2);
        writer.close();
        reader = DirectoryReader.open(directory);
        corpus = new LuceneTermCorpus(() -> reader, "f1");
    }

    @AfterClass
    public static void closeIndex() throws IOException {
        reader.close();
        directory.close();
    }

    @Test
    public void existsReturnsTrueForKnownTerm() {
        assertTrue(corpus.exists("apple"));
    }

    @Test
    public void existsReturnsFalseForUnknownTerm() {
        assertFalse(corpus.exists("mango"));
    }

    @Test
    public void docFreqReturnsCorrectCount() {
        assertEquals(5, corpus.docFreq("apple"));
        assertEquals(5, corpus.docFreq("banana"));
        assertEquals(3, corpus.docFreq("cherry"));
    }

    @Test
    public void docFreqReturnsZeroForUnknownTerm() {
        assertEquals(0, corpus.docFreq("mango"));
    }

    @Test
    public void numDocsReturnsTotalDocumentCount() {
        // 5 apple-banana docs + 3 cherry docs + 2 f2-apple docs = 10
        assertEquals(10, corpus.numDocs());
    }

    @Test
    public void coExistReturnsTrueForTermsInSameDocument() {
        assertTrue(corpus.coExist("apple", "banana"));
    }

    @Test
    public void coExistReturnsFalseForTermsNeverInSameDocument() {
        assertFalse(corpus.coExist("apple", "cherry"));
    }

    @Test
    public void coExistReturnsFalseWhenFirstTermDoesNotExist() {
        assertFalse(corpus.coExist("mango", "apple"));
    }

    @Test
    public void coExistReturnsFalseWhenSecondTermDoesNotExist() {
        assertFalse(corpus.coExist("apple", "mango"));
    }

    @Test
    public void coExistIgnoresTermsInOtherFields() {
        // "apple" exists in f2 but corpus is scoped to f1; "banana" only in f1
        // This confirms corpus uses only its configured field
        final LuceneTermCorpus f2Corpus = new LuceneTermCorpus(() -> reader, "f2");
        assertFalse(f2Corpus.coExist("apple", "banana"));
    }
}
