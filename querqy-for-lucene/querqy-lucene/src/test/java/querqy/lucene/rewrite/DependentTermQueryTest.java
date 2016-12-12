package querqy.lucene.rewrite;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by rene on 04/09/2016.
 */
public class DependentTermQueryTest extends LuceneTestCase {

    FieldBoost fieldBoost1 = new ConstantFieldBoost(1f);


    FieldBoost fieldBoost2  = new ConstantFieldBoost(2f);

    DocumentFrequencyAndTermContextProvider dfc1 = mock(DocumentFrequencyAndTermContextProvider.class);

    DocumentFrequencyAndTermContextProvider dfc2 = mock(DocumentFrequencyAndTermContextProvider.class);

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
    public void testThatHashCodeAndEqualDoNotDependOnDfc() throws Exception {

        DependentTermQuery tq1 = new DependentTermQuery(term1, dfc1, tqIndex1, fieldBoost1);
        DependentTermQuery tq2 = new DependentTermQuery(term1, dfc2, tqIndex1, fieldBoost1);
        assertEquals(tq1.hashCode(), tq2.hashCode());

        assertEquals(tq1, tq2);
    }

    @Test
    public void testThatHashCodeAndEqualDependOnTerm() throws Exception {

        DependentTermQuery tq1 = new DependentTermQuery(term1, dfc1, tqIndex1, fieldBoost1);
        DependentTermQuery tq2 = new DependentTermQuery(term2, dfc1, tqIndex1, fieldBoost1);
        assertNotEquals(tq1.hashCode(), tq2.hashCode());

        assertNotEquals(tq1, tq2);

    }

    @Test
    public void testThatHashCodeAndEqualDependOnTqIndex() throws Exception {

        DependentTermQuery tq1 = new DependentTermQuery(term1, dfc1, tqIndex1, fieldBoost1);
        DependentTermQuery tq2 = new DependentTermQuery(term1, dfc1, tqIndex2, fieldBoost1);
        assertNotEquals(tq1.hashCode(), tq2.hashCode());

        assertNotEquals(tq1, tq2);

    }

    @Test
    public void testThatHashCodeAndEqualDependOnFieldBoost() throws Exception {

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

        TestUtil.addNumDocs("f1", "v1", indexWriter, 1);
        TestUtil.addNumDocs("f1", "v2", indexWriter, 1);

        indexWriter.close();

        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = newSearcher(indexReader);

        DocumentFrequencyCorrection dfc = new DocumentFrequencyCorrection();

        Term term = new Term("f1", "v1");

        dfc.prepareTerm(term);

        DependentTermQuery query = new DependentTermQuery(term, dfc, fieldBoost);

        TopDocs topDocs = indexSearcher.search(query, 10);

        assertEquals(1, topDocs.totalHits);
        Document resultDoc = indexSearcher.doc(topDocs.scoreDocs[0].doc);
        assertEquals("v1", resultDoc.get("f1"));

        indexReader.close();
        directory.close();
        analyzer.close();

    }



}
