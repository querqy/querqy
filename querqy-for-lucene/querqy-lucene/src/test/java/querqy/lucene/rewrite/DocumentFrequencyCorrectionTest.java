package querqy.lucene.rewrite;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Test;
import querqy.lucene.rewrite.DocumentFrequencyAndTermContextProvider.DocumentFrequencyAndTermContext;

import static querqy.lucene.rewrite.TestUtil.addNumDocs;
import static querqy.lucene.rewrite.TestUtil.newTerm;

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
        Term term = new Term("f1", "a");

        // the term gets registered with the dfc
        dfc.prepareTerm(term);

        DependentTermQuery tq = new DependentTermQuery(term, dfc, ConstantFieldBoost.NORM_BOOST);
        dfc.finishedUserQuery();
        DocumentFrequencyAndTermContext documentFrequencyAndTermContext = dfc.getDocumentFrequencyAndTermContext(tq.tqIndex, indexSearcher.getTopReaderContext());
        
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
        
        Term t1 = newTerm("f1", "a", dfc);
        dfc.newClause();
        dfc.newClause();

        Term t2 = newTerm("f1", "b", dfc);

        dfc.newClause();
        dfc.newClause();
        dfc.finishedUserQuery();
        dfc.newClause();

        Term t1a = newTerm("f1", "a", dfc);
        Term t2a = newTerm("f1", "b", dfc);

        dfc.newClause();
        dfc.newClause();

        DependentTermQuery tq1 = new DependentTermQuery(t1, dfc, ConstantFieldBoost.NORM_BOOST);
        DependentTermQuery tq2 = new DependentTermQuery(t2, dfc, ConstantFieldBoost.NORM_BOOST);
        DependentTermQuery tq1a = new DependentTermQuery(t1a, dfc, ConstantFieldBoost.NORM_BOOST);
        DependentTermQuery tq2a = new DependentTermQuery(t2a, dfc, ConstantFieldBoost.NORM_BOOST);

        
        DocumentFrequencyAndTermContext dftc1 = dfc.getDocumentFrequencyAndTermContext(tq1.tqIndex,
                indexSearcher.getTopReaderContext());
        DocumentFrequencyAndTermContext dftc2 = dfc.getDocumentFrequencyAndTermContext(tq2.tqIndex,
                indexSearcher.getTopReaderContext());
        DocumentFrequencyAndTermContext dftc1a = dfc.getDocumentFrequencyAndTermContext(tq1a.tqIndex,
                indexSearcher.getTopReaderContext());
        DocumentFrequencyAndTermContext dftc2a = dfc.getDocumentFrequencyAndTermContext(tq2a.tqIndex,
                indexSearcher.getTopReaderContext());

        assertEquals(df1, dftc1.termContext.docFreq());
        assertEquals(df2, dftc2.termContext.docFreq());
        assertEquals(df2 * 2 - 1, dftc1a.termContext.docFreq()); // df = max in clause + max in user query - 1
        assertEquals(df2 * 2 - 1, dftc2a.termContext.docFreq());
        
        
        indexReader.close();
        directory.close();
        analyzer.close();
        
        
    }

    int getRandomDf() {
        return 1 + new Long(Math.round(50.0 * Math.random())).intValue();
    }

}
