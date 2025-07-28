package querqy.lucene.rewrite;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.tests.index.RandomIndexWriter;
import org.apache.lucene.tests.util.LuceneTestCase;
import org.junit.Test;
import querqy.lucene.rewrite.SimilarityTermQueryBuilder.SimilarityTermQuery;

public class SimilarityTermQueryBuilderTest extends LuceneTestCase {

    FieldBoost fieldBoost1  = new ConstantFieldBoost(1f);
    FieldBoost fieldBoost2  = new ConstantFieldBoost(2f);

    @Test
    public void testThatThereIsNoDocumentFrequencyCorrection() {
        assertFalse(new SimilarityTermQueryBuilder().getDocumentFrequencyCorrection().isPresent());
    }

    // ***** Query *****

    @Test
    public void testThatHashCodeAndEqualsDependOnTerm() {

        SimilarityTermQuery tq1 = new SimilarityTermQueryBuilder()
                .createTermQuery(new Term("f1", "t1"), fieldBoost1);
        SimilarityTermQuery tq2 = new SimilarityTermQueryBuilder()
                .createTermQuery(new Term("f1", "t2"), fieldBoost1);
        assertNotEquals(tq1.hashCode(), tq2.hashCode());
        assertNotEquals(tq1, tq2);

    }

    @Test
    public void testThatHashCodeAndEqualDependOnFieldBoost() {

        SimilarityTermQuery tq1 = new SimilarityTermQueryBuilder()
                .createTermQuery(new Term("f1", "t1"), fieldBoost1);
        SimilarityTermQuery tq2 = new SimilarityTermQueryBuilder()
                .createTermQuery(new Term("f1", "t1"), fieldBoost2);
        assertNotEquals(tq1.hashCode(), tq2.hashCode());
        assertNotEquals(tq1, tq2);

    }

    @Test
    public void testThatHashCodeAndEqualMatch() {

        SimilarityTermQuery tq1 = new SimilarityTermQueryBuilder()
                .createTermQuery(new Term("f1", "t1"), fieldBoost2);
        SimilarityTermQuery tq2 = new SimilarityTermQueryBuilder()
                .createTermQuery(new Term("f1", "t1"), fieldBoost2);
        assertEquals(tq1.hashCode(), tq2.hashCode());
        assertEquals(tq1, tq2);

    }

    @Test
    public void testThatResultsAreFound() throws Exception {

        Analyzer analyzer = new KeywordAnalyzer();

        Directory directory = newDirectory();
        RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        TestUtil.addNumDocsWithStringField("f1", "v1", indexWriter, 1);
        TestUtil.addNumDocsWithStringField("f1", "v2", indexWriter, 1);

        indexWriter.close();

        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = newSearcher(indexReader);


        Term term = new Term("f1", "v1");

        SimilarityTermQuery query = new SimilarityTermQueryBuilder()
                .createTermQuery(term, fieldBoost1);

        TopDocs topDocs = indexSearcher.search(query, 10);

        assertEquals(1, topDocs.totalHits.value());
        StoredFields storedFields = indexReader.storedFields();
        Document resultDoc = storedFields.document(topDocs.scoreDocs[0].doc);
        assertEquals("v1", resultDoc.get("f1"));

        indexReader.close();
        directory.close();
        analyzer.close();

    }

    @Test
    public void testCreateWeight() throws Exception {

        Analyzer analyzer = new StandardAnalyzer();

        Directory directory = new ByteBuffersDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setSimilarity(new ClassicSimilarity());
        IndexWriter indexWriter = new IndexWriter(directory, config);


        TestUtil.addNumDocsWithTextField("f1", "v1 v1", indexWriter, 4);
        TestUtil.addNumDocsWithTextField("f1", "v2", indexWriter, 1);

        indexWriter.close();

        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        indexSearcher.setSimilarity(new ClassicSimilarity());


        Term term = new Term("f1", "v1");

        SimilarityTermQuery query = new SimilarityTermQueryBuilder().createTermQuery(term, fieldBoost2);

        TopDocs topDocs = indexSearcher.search(query, 10);

        final Weight weight = query.createWeight(indexSearcher, ScoreMode.COMPLETE, 4.5f);
        final Explanation explain = weight.explain(indexReader.getContext().leaves().get(0), topDocs.scoreDocs[0].doc);

        String explainText = explain.toString();

        assertTrue(explainText.contains("9.0 = boost")); // 4.5 (query) * 2.0 (field)
        assertTrue(explainText.contains("4 = docFreq")); // 4 * v1
        assertTrue(explainText.contains("2.0 = freq")); // 2 * v1 in field

        indexReader.close();
        directory.close();
        analyzer.close();

    }
}
