package querqy.lucene.rewrite;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Test;

import querqy.lucene.rewrite.DocumentFrequencyCorrection.DocumentFrequencyAndTermContext;

public class DocumentFrequencyCorrectionTest extends LuceneTestCase {

    @Test
    public void testGetDf() throws Exception {
        
        Analyzer analyzer = new MockAnalyzer(random());

        Directory directory = newDirectory();
        RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);
       
        int df = getRandomDf();
        addNumDocs("f1", "a", indexWriter, df);
        
        indexWriter.close();
        
        IndexReader indexReader = DirectoryReader.open(directory); 
        IndexSearcher indexSearcher = newSearcher(indexReader);
        
        DocumentFrequencyCorrection dfc = new DocumentFrequencyCorrection();
        dfc.newClause();
        // the term query registers itself with the dfc
        DocumentFrequencyCorrectedTermQuery tq = new DocumentFrequencyCorrectedTermQuery(new Term("f1", "a"), dfc);
        dfc.finishedUserQuery();
        DocumentFrequencyAndTermContext documentFrequencyAndTermContext = dfc.getDocumentFrequencyAndTermContext(tq.tqIndex, indexSearcher);
        
        assertEquals(df, documentFrequencyAndTermContext.termContext.docFreq());
        
        indexReader.close();
        directory.close();
        analyzer.close();
        
        
    }
    
    @Test
    public void testEmptyClauses() throws Exception {
        
        Analyzer analyzer = new MockAnalyzer(random());

        Directory directory = newDirectory();
        RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);
       
        int df1 = getRandomDf();
        addNumDocs("f1", "a", indexWriter, df1);
        
        int df2 = df1 + 5;
        addNumDocs("f1", "b", indexWriter, df2);
        
        indexWriter.close();
        
        
        IndexReader indexReader = DirectoryReader.open(directory); 
        IndexSearcher indexSearcher = newSearcher(indexReader);
        
        DocumentFrequencyCorrection dfc = new DocumentFrequencyCorrection();
        dfc.newClause();
        dfc.newClause();
        
        // the term query registers itself with the dfc
        DocumentFrequencyCorrectedTermQuery tq1 = new DocumentFrequencyCorrectedTermQuery(new Term("f1", "a"), dfc);
        dfc.newClause();
        dfc.newClause();
        DocumentFrequencyCorrectedTermQuery tq2 = new DocumentFrequencyCorrectedTermQuery(new Term("f1", "b"), dfc);
        dfc.newClause();
        dfc.newClause();
        dfc.finishedUserQuery();
        dfc.newClause();
        DocumentFrequencyCorrectedTermQuery tq1a = new DocumentFrequencyCorrectedTermQuery(new Term("f1", "a"), dfc);
        DocumentFrequencyCorrectedTermQuery tq2a = new DocumentFrequencyCorrectedTermQuery(new Term("f1", "b"), dfc);
        dfc.newClause();
        dfc.newClause();
        
        DocumentFrequencyAndTermContext dftc1 = dfc.getDocumentFrequencyAndTermContext(tq1.tqIndex, indexSearcher);
        DocumentFrequencyAndTermContext dftc2 = dfc.getDocumentFrequencyAndTermContext(tq2.tqIndex, indexSearcher);
        DocumentFrequencyAndTermContext dftc1a = dfc.getDocumentFrequencyAndTermContext(tq1a.tqIndex, indexSearcher);
        DocumentFrequencyAndTermContext dftc2a = dfc.getDocumentFrequencyAndTermContext(tq2a.tqIndex, indexSearcher);
        
        assertEquals(df1, dftc1.termContext.docFreq());
        assertEquals(df2, dftc2.termContext.docFreq());
        assertEquals(df2 * 2 - 1, dftc1a.termContext.docFreq()); // df = max in clause + max in user query - 1
        assertEquals(df2 * 2 - 1, dftc2a.termContext.docFreq());
        
        
        indexReader.close();
        directory.close();
        analyzer.close();
        
        
    }
    
    void addNumDocs(String fieldname, String value, RandomIndexWriter indexWriter, int num) throws IOException {
        for (int i = 0; i < num; i++) {
            Document doc = new Document();
            doc.add(newStringField(fieldname, value, Store.YES));
            indexWriter.addDocument(doc);
        }
    }
    
    int getRandomDf() {
        return 1 + new Long(Math.round(50.0 * Math.random())).intValue();
    }

}
