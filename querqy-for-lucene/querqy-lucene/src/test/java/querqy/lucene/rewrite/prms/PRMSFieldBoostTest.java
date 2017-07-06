package querqy.lucene.rewrite.prms;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Before;
import org.junit.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import querqy.lucene.rewrite.*;
import querqy.lucene.rewrite.SearchFieldsAndBoosting.FieldBoostModel;
import querqy.parser.WhiteSpaceQuerqyParser;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;

public class PRMSFieldBoostTest extends LuceneTestCase {

    Similarity similarity;

    Similarity.SimWeight simWeight;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        similarity = Mockito.mock(Similarity.class);
        simWeight = Mockito.mock(Similarity.SimWeight.class);
        Mockito.when(similarity.computeWeight(any(CollectionStatistics.class),  Matchers.<TermStatistics>anyVararg())).thenReturn(simWeight);
    }

    @Test
    public void testGetThatFieldProbabilityRatioIsReflectedInBoost() throws Exception {

        ArgumentCaptor<Float> normalizeCaptor = ArgumentCaptor.forClass(Float.class);
        
        DocumentFrequencyCorrection dfc = new DocumentFrequencyCorrection();

        Directory directory = newDirectory();
        
        Analyzer analyzer = new StandardAnalyzer();

        IndexWriterConfig conf = new IndexWriterConfig(analyzer);
        conf.setCodec(Codec.forName(TestUtil.LUCENE_CODEC));
        IndexWriter indexWriter = new IndexWriter(directory, conf);
       
        addNumDocs("f1", "abc", indexWriter, 2);
        addNumDocs("f1", "def", indexWriter, 4);
        addNumDocs("f2", "abc", indexWriter, 4);
        addNumDocs("f2", "def", indexWriter, 2);
        indexWriter.close();
        
        IndexReader indexReader = DirectoryReader.open(directory); 
        IndexSearcher indexSearcher =  new IndexSearcher(indexReader);
        indexSearcher.setSimilarity(similarity);
        
        Map<String, Float> fields = new HashMap<>();
        fields.put("f1", 1f);
        fields.put("f2", 1f);
        SearchFieldsAndBoosting searchFieldsAndBoosting = new SearchFieldsAndBoosting(FieldBoostModel.PRMS, fields, fields, 0.8f);
        
        LuceneQueryBuilder queryBuilder = new LuceneQueryBuilder(dfc, analyzer, searchFieldsAndBoosting, 0.01f, null);
        
        WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
        
        Query query = queryBuilder.createQuery(parser.parse("abc")).rewrite(indexReader);
        dfc.finishedUserQuery();
        //query.createWeight(indexSearcher, true);
        assertTrue(query instanceof DisjunctionMaxQuery);
        
        DisjunctionMaxQuery dmq = (DisjunctionMaxQuery) query;
        List<Query> disjuncts = dmq.getDisjuncts();
        assertEquals(2, disjuncts.size());
        
        Query disjunct1 = disjuncts.get(0);
        assertTrue(disjunct1 instanceof DependentTermQuery);
        DependentTermQuery tq1 = (DependentTermQuery) disjunct1;
        
        Query disjunct2 = disjuncts.get(1);
        assertTrue(disjunct2 instanceof DependentTermQuery);
        DependentTermQuery tq2 = (DependentTermQuery) disjunct2;

        assertNotEquals(tq1.getTerm().field(), tq2.getTerm().field());

        final Weight weight1 = disjunct1.createWeight(indexSearcher, true);
        final Weight weight2 = disjunct2.createWeight(indexSearcher, true);
        weight1.normalize(0.1f, 5f);
        weight2.normalize(0.1f, 5f);

        Mockito.verify(simWeight, times(2)).normalize(eq(0.1f), normalizeCaptor.capture());
        final List<Float> capturedBoosts = normalizeCaptor.getAllValues();
        float bf1 = capturedBoosts.get(0);
        float bf2 = capturedBoosts.get(1);

        assertEquals(2f, bf2 / bf1, 0.00001);

        indexReader.close();
        directory.close();
        analyzer.close();
        
        
    }
    
    public static void addNumDocs(String fieldname, String value, IndexWriter indexWriter, int num) throws IOException {
        for (int i = 0; i < num; i++) {
            Document doc = new Document();
            doc.add(new TextField(fieldname, value, Store.YES));
            indexWriter.addDocument(doc);
        }
    }

}
