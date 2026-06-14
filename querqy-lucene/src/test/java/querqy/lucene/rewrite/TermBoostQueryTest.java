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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.tests.analysis.MockAnalyzer;
import org.apache.lucene.tests.index.RandomIndexWriter;
import org.apache.lucene.tests.util.LuceneTestCase;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Created by rene on 11/09/2016.
 */
public class TermBoostQueryTest extends LuceneTestCase {

    @Test
    public void testThatWeightGetsScoreFromFieldBoost() throws Exception {

        final float fieldBoostFactor = 2f;

        ConstantFieldBoost fieldBoost = new ConstantFieldBoost(fieldBoostFactor);

        Analyzer analyzer = new MockAnalyzer(random());

        Directory directory = newDirectory();
        RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        indexWriter.close();


        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = newSearcher(indexReader);

        final FieldBoostTermQueryBuilder.FieldBoostTermQuery tbq = new FieldBoostTermQueryBuilder.FieldBoostTermQuery(new Term("f1", "v1"), fieldBoost);

        final Weight weight = tbq.createWeight(indexSearcher, ScoreMode.COMPLETE, 1f);

        assertTrue(weight instanceof FieldBoostTermQueryBuilder.FieldBoostTermQuery.FieldBoostWeight);
        final FieldBoostTermQueryBuilder.FieldBoostTermQuery.FieldBoostWeight tbw = (FieldBoostTermQueryBuilder.FieldBoostTermQuery.FieldBoostWeight) weight;

        assertEquals(fieldBoostFactor, tbw.getFieldBoost(), 0.0001f);

        indexReader.close();
        directory.close();
        analyzer.close();

    }

    @Test
    public void testThatExternalBoostFactorIsApplied() throws Exception {

        final float fieldBoostFactor = 2f;
        final float externalBoostFactor = 3f;

        ConstantFieldBoost fieldBoost = new ConstantFieldBoost(fieldBoostFactor);

        Analyzer analyzer = new MockAnalyzer(random());

        Directory directory = newDirectory();
        RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        indexWriter.close();


        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = newSearcher(indexReader);

        final FieldBoostTermQueryBuilder.FieldBoostTermQuery tbq = new FieldBoostTermQueryBuilder.FieldBoostTermQuery(new Term("f1", "v1"), fieldBoost);

        final Weight weight = tbq.createWeight(indexSearcher, ScoreMode.COMPLETE, externalBoostFactor);

        assertTrue(weight instanceof FieldBoostTermQueryBuilder.FieldBoostTermQuery.FieldBoostWeight);
        final FieldBoostTermQueryBuilder.FieldBoostTermQuery.FieldBoostWeight tbw = (FieldBoostTermQueryBuilder.FieldBoostTermQuery.FieldBoostWeight) weight;

        assertEquals(fieldBoostFactor * externalBoostFactor, tbw.getScore(), 0.0001f);

        indexReader.close();
        directory.close();
        analyzer.close();

    }

    @Test
    public void testThatSimilarityIsNotUsedForCollectionStats() throws Exception {

        ConstantFieldBoost fieldBoost = new ConstantFieldBoost(1f);

        Analyzer analyzer = new MockAnalyzer(random());

        Directory directory = newDirectory();
        RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        TestUtil.addNumDocsWithStringField("f1", "v1", indexWriter, 1);

        indexWriter.close();


        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = newSearcher(indexReader);

        Similarity similarity = mock(Similarity.class);
        indexSearcher.setSimilarity(similarity);
        FieldBoostTermQueryBuilder.FieldBoostTermQuery termBoostQuery = new FieldBoostTermQueryBuilder.FieldBoostTermQuery(new Term("f1", "v1"), fieldBoost);
        indexSearcher.search(termBoostQuery, 10);

        verify(similarity, never()).scorer(
                ArgumentMatchers.anyFloat(),
                ArgumentMatchers.any(CollectionStatistics.class),
                ArgumentMatchers.<TermStatistics>any()
                );



        indexReader.close();
        directory.close();
        analyzer.close();

    }

    @Test
    public void testThatResultsAreStillFound() throws Exception {
        ConstantFieldBoost fieldBoost = new ConstantFieldBoost(1f);

        Analyzer analyzer = new KeywordAnalyzer();

        Directory directory = newDirectory();
        RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        TestUtil.addNumDocsWithStringField("f1", "v1", indexWriter, 1);
        TestUtil.addNumDocsWithStringField("f1", "v2", indexWriter, 1);

        indexWriter.close();


        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = newSearcher(indexReader);

        FieldBoostTermQueryBuilder.FieldBoostTermQuery termBoostQuery = new FieldBoostTermQueryBuilder.FieldBoostTermQuery(new Term("f1", "v1"), fieldBoost);
        TopDocs topDocs = indexSearcher.search(termBoostQuery, 10);

        assertEquals(1, topDocs.totalHits.value());
        StoredFields storedFields = indexReader.storedFields();
        Document resultDoc = storedFields.document(topDocs.scoreDocs[0].doc);
        assertEquals("v1", resultDoc.get("f1"));

        indexReader.close();
        directory.close();
        analyzer.close();

    }
}
