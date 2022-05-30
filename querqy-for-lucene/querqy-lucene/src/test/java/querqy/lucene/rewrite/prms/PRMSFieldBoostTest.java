package querqy.lucene.rewrite.prms;

import java.io.IOException;
import java.util.ArrayList;
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
import org.junit.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import querqy.lucene.rewrite.DependentTermQueryBuilder;
import querqy.lucene.rewrite.DocumentFrequencyCorrection;
import querqy.lucene.rewrite.LuceneQueryBuilder;
import querqy.lucene.rewrite.SearchFieldsAndBoosting;
import querqy.lucene.rewrite.SearchFieldsAndBoosting.FieldBoostModel;
import querqy.lucene.rewrite.TestUtil;
import querqy.parser.WhiteSpaceQuerqyParser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class PRMSFieldBoostTest extends LuceneTestCase {

    @Test
    public void testGetThatFieldProbabilityRatioIsReflectedInBoost() throws Exception {

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

        
        Map<String, Float> fields = new HashMap<>();
        fields.put("f1", 1f);
        fields.put("f2", 1f);
        SearchFieldsAndBoosting searchFieldsAndBoosting = new SearchFieldsAndBoosting(FieldBoostModel.PRMS, fields, fields, 0.8f);
        
        LuceneQueryBuilder queryBuilder = new LuceneQueryBuilder(new DependentTermQueryBuilder(dfc), analyzer,
                searchFieldsAndBoosting, 0.01f, 1f, null);
        
        WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
        
        Query query = queryBuilder.createQuery(parser.parse("abc"));
        dfc.finishedUserQuery();
        
        assertTrue(query instanceof DisjunctionMaxQuery);
        
        DisjunctionMaxQuery dmq = (DisjunctionMaxQuery) query;
        List<Query> disjuncts = new ArrayList<>(dmq.getDisjuncts());
        assertEquals(2, disjuncts.size());
        
        Query disjunct1 = disjuncts.get(0);
        assertTrue(disjunct1 instanceof DependentTermQueryBuilder.DependentTermQuery);
        DependentTermQueryBuilder.DependentTermQuery dtq1 = (DependentTermQueryBuilder.DependentTermQuery) disjunct1;
        
        Query disjunct2 = disjuncts.get(1);
        assertTrue(disjunct2 instanceof DependentTermQueryBuilder.DependentTermQuery);
        DependentTermQueryBuilder.DependentTermQuery dtq2 = (DependentTermQueryBuilder.DependentTermQuery) disjunct2;

        assertNotEquals(dtq1.getTerm().field(), dtq2.getTerm().field());

        Similarity similarity = Mockito.mock(Similarity.class);
        Similarity.SimScorer simScorer = Mockito.mock(Similarity.SimScorer.class);

        ArgumentCaptor<Float> computeWeightBoostCaptor = ArgumentCaptor.forClass(Float.class);

        when(similarity.scorer(
                computeWeightBoostCaptor.capture(),
                any(CollectionStatistics.class),
                ArgumentMatchers.<TermStatistics>any())).thenReturn(simScorer);

        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher =  new IndexSearcher(indexReader);
        indexSearcher.setSimilarity(similarity);

        Weight weight1 = indexSearcher.createWeight(dtq1, ScoreMode.COMPLETE, 1.0f);
        Weight weight2 = indexSearcher.createWeight(dtq2, ScoreMode.COMPLETE, 1.0f);

        final List<Float> capturedBoosts = computeWeightBoostCaptor.getAllValues();
        float bf1 = capturedBoosts.get(0);
        float bf2 = capturedBoosts.get(1);

        final String field1 = ((DependentTermQueryBuilder.DependentTermQuery) disjunct1).getTerm().field();
        // Dismax clauses are a set - we have no guarantee about order -> identify by field name

        assertEquals("f1".equals(field1) ? 2f : 0.5f, bf2 / bf1, 0.00001);

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
