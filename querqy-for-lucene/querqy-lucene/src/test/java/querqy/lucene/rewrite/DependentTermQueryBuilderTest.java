package querqy.lucene.rewrite;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Before;
import org.junit.Test;
import querqy.lucene.rewrite.DependentTermQueryBuilder.DependentTermQuery;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by rene on 04/09/2016.
 */
public class DependentTermQueryBuilderTest extends LuceneTestCase {

    FieldBoost fieldBoost1 = new ConstantFieldBoost(1f);
    FieldBoost fieldBoost2  = new ConstantFieldBoost(2f);

    DocumentFrequencyCorrection dfc1 = mock(DocumentFrequencyCorrection.class);
    DocumentFrequencyCorrection dfc2 = mock(DocumentFrequencyCorrection.class);

    Term term1 = new Term("f1", "t1");
    Term term2 = new Term("f1", "t2");

    int tqIndex1 = 1;
    int tqIndex2 = 2;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(dfc1.termIndex()).thenReturn(tqIndex1);
        when(dfc2.termIndex()).thenReturn(tqIndex2);
    }

    @Test
    public void testThatHashCodeAndEqualDoNotDependOnDfc() {

        DependentTermQuery tq1 = new DependentTermQuery(term1, dfc1, tqIndex1, fieldBoost1);
        DependentTermQuery tq2 = new DependentTermQuery(term1, dfc2, tqIndex1, fieldBoost1);
        assertEquals(tq1.hashCode(), tq2.hashCode());

        assertEquals(tq1, tq2);
    }

    @Test
    public void testThatHashCodeAndEqualDependOnTerm() {

        DependentTermQuery tq1 = new DependentTermQuery(term1, dfc1, tqIndex1, fieldBoost1);
        DependentTermQuery tq2 = new DependentTermQuery(term2, dfc1, tqIndex1, fieldBoost1);
        assertNotEquals(tq1.hashCode(), tq2.hashCode());

        assertNotEquals(tq1, tq2);

    }

    @Test
    public void testThatHashCodeAndEqualDependOnTqIndex() {

        DependentTermQuery tq1 = new DependentTermQuery(term1, dfc1, tqIndex1, fieldBoost1);
        DependentTermQuery tq2 = new DependentTermQuery(term1, dfc1, tqIndex2, fieldBoost1);
        assertNotEquals(tq1.hashCode(), tq2.hashCode());

        assertNotEquals(tq1, tq2);

    }

    @Test
    public void testThatHashCodeAndEqualDependOnFieldBoost() {

        DependentTermQuery tq1 = new DependentTermQuery(term1, dfc1, tqIndex1, fieldBoost1);
        DependentTermQuery tq2 = new DependentTermQuery(term1, dfc1, tqIndex1, fieldBoost2);
        assertNotEquals(tq1.hashCode(), tq2.hashCode());

        assertNotEquals(tq1, tq2);

    }

    @Test
    public void testThatResultsAreFound() throws Exception {
        ConstantFieldBoost fieldBoost = new ConstantFieldBoost(1f);
        Analyzer analyzer = new KeywordAnalyzer();

        Directory directory = newDirectory();
        RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        TestUtil.addNumDocsWithStringField("f1", "v1", indexWriter, 1);
        TestUtil.addNumDocsWithStringField("f1", "v2", indexWriter, 1);

        indexWriter.close();

        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = newSearcher(indexReader);

        DocumentFrequencyCorrection dfc = new DocumentFrequencyCorrection();

        Term term = new Term("f1", "v1");
        dfc.newClause();
        dfc.prepareTerm(term);
        dfc.finishedUserQuery();

        DependentTermQuery query = new DependentTermQuery(term, dfc, fieldBoost);

        TopDocs topDocs = indexSearcher.search(query, 10);

        assertEquals(1, topDocs.totalHits.value);
        Document resultDoc = indexSearcher.doc(topDocs.scoreDocs[0].doc);
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

        TestUtil.addNumDocsWithTextField("f1", "v1", indexWriter, 4);
        TestUtil.addNumDocsWithTextField("f2", "v1 v1", indexWriter, 1);

        indexWriter.close();

        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        indexSearcher.setSimilarity(new ClassicSimilarity());


        DocumentFrequencyCorrection dfc = new DocumentFrequencyCorrection();

        Term qTerm1 = new Term("f1", "v1");
        Term qTerm2 = new Term("f2", "v1");
        dfc.newClause();
        dfc.prepareTerm(qTerm1);
        dfc.prepareTerm(qTerm2);
        dfc.finishedUserQuery();

        DependentTermQueryBuilder.DependentTermQuery query1 = new DependentTermQueryBuilder(dfc)
                .createTermQuery(qTerm1, fieldBoost1);
        DependentTermQueryBuilder.DependentTermQuery query2 = new DependentTermQueryBuilder(dfc)
                .createTermQuery(qTerm2, fieldBoost2);


        TopDocs topDocs = indexSearcher.search(query2, 10);

        final Weight weight2 = query2.createWeight(indexSearcher, ScoreMode.COMPLETE, 4.5f);
        final Explanation explain = weight2.explain(indexReader.leaves().get(0), topDocs.scoreDocs[0].doc);

        String explainText = explain.toString();
        assertTrue(explainText.contains("9.0 = boost")); // 4.5 (query) * 2.0 (field)
        assertTrue(explainText.contains("4 = docFreq")); // 4 * df of f1:v1
        assertTrue(explainText.contains("2.0 = freq")); // don't use tf

        indexReader.close();
        directory.close();
        analyzer.close();

    }

    @Test
    public void testPostingsVsMaxScore() throws Exception {

        Analyzer analyzer = new StandardAnalyzer();

        Directory directory = new ByteBuffersDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setSimilarity(new ClassicSimilarity());
        IndexWriter indexWriter = new IndexWriter(directory, config);

        TestUtil.addNumDocsWithTextField("f1", "v1", indexWriter, 1);
        TestUtil.addNumDocsWithTextField("f2", "v1 v2", indexWriter, 1);


        indexWriter.close();

        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        indexSearcher.setSimilarity(new ClassicSimilarity());


        DocumentFrequencyCorrection dfc = new DocumentFrequencyCorrection();

        Term qTerm1 = new Term("f2", "v1");
        Term qTerm2 = new Term("f2", "v2");
        dfc.newClause();
        dfc.prepareTerm(qTerm1);
        dfc.newClause();
        dfc.prepareTerm(qTerm2);
        dfc.finishedUserQuery();


        DependentTermQueryBuilder.DependentTermQuery query1 = new DependentTermQueryBuilder(dfc)
                .createTermQuery(qTerm1, fieldBoost1);
        DependentTermQueryBuilder.DependentTermQuery query2 = new DependentTermQueryBuilder(dfc)
                .createTermQuery(qTerm2, fieldBoost2);


        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(query1, BooleanClause.Occur.SHOULD);
        builder.add(query2, BooleanClause.Occur.SHOULD);
        builder.setMinimumNumberShouldMatch(2);

        BooleanQuery bq = builder.build();

        // Query execution will call org.apache.lucene.search.Scorer.getMaxScore which might consume
        // the postingsEnum so that we don't get any hit
        TopDocs topDocs = indexSearcher.search(bq, 10);
        assertEquals(1, topDocs.scoreDocs.length);



    }

}
