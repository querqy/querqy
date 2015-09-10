package querqy.lucene.rewrite.prms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Test;

import querqy.lucene.rewrite.DependentTermQuery;
import querqy.lucene.rewrite.DocumentFrequencyCorrection;
import querqy.lucene.rewrite.LuceneQueryBuilder;
import querqy.lucene.rewrite.SearchFieldsAndBoosting;
import querqy.lucene.rewrite.SearchFieldsAndBoosting.FieldBoostModel;
import querqy.parser.WhiteSpaceQuerqyParser;

public class PRMSFieldBoostTest extends LuceneTestCase {

    @Test
    public void testGetThatFieldProbabilityRatioIsReflectedInBoost() throws Exception {
        
        DocumentFrequencyCorrection dfc = new DocumentFrequencyCorrection();

        Directory directory = newDirectory();
        
        Analyzer analyzer = new StandardAnalyzer();

        IndexWriterConfig conf = new IndexWriterConfig(analyzer);
        conf.setCodec(Codec.forName("Lucene50"));
        IndexWriter indexWriter = new IndexWriter(directory, conf);
       
        addNumDocs("f1", "abc", indexWriter, 2);
        addNumDocs("f1", "def", indexWriter, 4);
        addNumDocs("f2", "abc", indexWriter, 4);
        addNumDocs("f2", "def", indexWriter, 2);
        indexWriter.close();
        
        IndexReader indexReader = DirectoryReader.open(directory); 
        IndexSearcher indexSearcher =  new IndexSearcher(indexReader);
        
        Map<String, Float> fields = new HashMap<>();
        fields.put("f1", 1f);
        fields.put("f2", 1f);
        SearchFieldsAndBoosting searchFieldsAndBoosting = new SearchFieldsAndBoosting(FieldBoostModel.PRMS, fields, fields, 0.8f);
        
        LuceneQueryBuilder queryBuilder = new LuceneQueryBuilder(dfc, analyzer, searchFieldsAndBoosting, 0.01f, null);
        
        WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
        
        Query query = queryBuilder.createQuery(parser.parse("abc"));
        dfc.finishedUserQuery();
        query.createWeight(indexSearcher, true);
        assertTrue(query instanceof DisjunctionMaxQuery);
        
        DisjunctionMaxQuery dmq = (DisjunctionMaxQuery) query;
        ArrayList<Query> disjuncts = dmq.getDisjuncts();
        assertEquals(2, disjuncts.size());
        
        Query disjunct1 = disjuncts.get(0);
        assertTrue(disjunct1 instanceof DependentTermQuery);
        DependentTermQuery dtq1 = (DependentTermQuery) disjunct1;
        
        Query disjunct2 = disjuncts.get(1);
        assertTrue(disjunct2 instanceof DependentTermQuery);
        DependentTermQuery dtq2 = (DependentTermQuery) disjunct2;
        
        assertNotEquals(dtq1.getTerm().field(), dtq2.getTerm().field());
        
        float bf1 = ("f1".equals(dtq1.getTerm().field())) ? dtq1.getBoostFactor() : dtq2.getBoostFactor();
        float bf2 = ("f2".equals(dtq2.getTerm().field())) ? dtq2.getBoostFactor() : dtq1.getBoostFactor();
        
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
