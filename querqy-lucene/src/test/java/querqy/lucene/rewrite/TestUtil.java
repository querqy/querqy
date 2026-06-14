/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2016 Querqy Contributors
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
package querqy.lucene.rewrite;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.tests.index.RandomIndexWriter;
import org.apache.lucene.tests.util.LuceneTestCase;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by rene on 14/09/2016.
 */
public class TestUtil {

    public static final String LUCENE_CODEC = System.getProperty("tests.codec");

    public static void addNumDocsWithStringField(final String fieldname, final String value,
                                                 final IndexWriter indexWriter, final int num) throws IOException {
        indexWriter.addDocuments(IntStream.range(0, num).mapToObj(i -> {

            final Document doc = new Document();
            doc.add(LuceneTestCase.newStringField(fieldname, value, Field.Store.YES));
            return doc;

        }).collect(Collectors.toList()));
    }

    public static void addNumDocsWithTextField(final String fieldname, final String value,
                                               final IndexWriter indexWriter, int num) throws IOException {
        indexWriter.addDocuments(IntStream.range(0, num).mapToObj(i -> {

            final Document doc = new Document();
            doc.add(LuceneTestCase.newTextField(fieldname, value, Field.Store.YES));
            return doc;

        }).collect(Collectors.toList()));
    }

    public static void addNumDocsWithStringField(final String fieldname, final String value,
                                                 final RandomIndexWriter indexWriter, final int num) throws IOException {

        indexWriter.addDocuments(IntStream.range(0, num).mapToObj(i -> {

            final Document doc = new Document();
            doc.add(LuceneTestCase.newStringField(fieldname, value, Field.Store.YES));
            return doc;

        }).collect(Collectors.toList()));

    }

    public static void addNumDocsWithTextField(final String fieldname, final String value,
                                               final RandomIndexWriter indexWriter, final int num) throws IOException {

        indexWriter.addDocuments(IntStream.range(0, num).mapToObj(i -> {

            final Document doc = new Document();
            doc.add(LuceneTestCase.newTextField(fieldname, value, Field.Store.YES));
            return doc;

        }).collect(Collectors.toList()));

    }

    public static Term newTerm(final String field, final String value, final DocumentFrequencyCorrection dfc) {
        Term term = new Term(field, value);
        dfc.prepareTerm(term);
        return term;
    }
}
