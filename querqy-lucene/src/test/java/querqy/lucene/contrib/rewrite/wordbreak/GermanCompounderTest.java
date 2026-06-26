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
package querqy.lucene.contrib.rewrite.wordbreak;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.tests.index.RandomIndexWriter;
import org.apache.lucene.tests.util.LuceneTestCase;
import org.junit.Test;
import querqy.lucene.LuceneTermCorpus;
import querqy.model.Term;
import querqy.rewrite.contrib.wordbreak.*;

import java.io.IOException;
import java.util.List;


public class GermanCompounderTest extends LuceneTestCase {

    @Test
    public void testWithEmptyIndex() throws IOException {
        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final String field = "f1";
            final MorphologicalCompounder compounder = new MorphologicalCompounder(new MorphologyProvider().get("GERMAN").get(), true, 1);
            final querqy.model.Term left = new querqy.model.Term(null, field, "left", false);
            final querqy.model.Term right = new querqy.model.Term(null, field, "left", false);
            final List<Compounder.CompoundTerm> sequences = compounder.combine(new Term[]{left, right}, new LuceneTermCorpus(() -> indexReader, field), false);
            assertNotNull(sequences);
            assertTrue(sequences.isEmpty());

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }
}
