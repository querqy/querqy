package querqy.lucene.rewrite.prms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Test;

import querqy.lucene.rewrite.DependentTermQuery;
import querqy.lucene.rewrite.DocumentFrequencyCorrection;
import querqy.lucene.rewrite.LuceneQueryBuilder;
import querqy.lucene.rewrite.SearchFieldsAndBoosting;
import querqy.lucene.rewrite.SearchFieldsAndBoosting.FieldBoostModel;
import querqy.parser.WhiteSpaceQuerqyParser;

public class PRMSAndQueryTest extends LuceneTestCase {

    @Test
    public void testGetThatFieldProbabilityRatioIsReflectedInBoost() throws Exception {
        
        DocumentFrequencyCorrection dfc = new DocumentFrequencyCorrection();

        Directory directory = newDirectory();
        
        
        Analyzer analyzer = new Analyzer() {
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new WhitespaceTokenizer();
                TokenStream filter = new WordDelimiterFilter(source,
                            WordDelimiterFilter.GENERATE_WORD_PARTS 
                            | WordDelimiterFilter.SPLIT_ON_CASE_CHANGE
                            , null);
                filter = new LowerCaseFilter(filter);
                return new TokenStreamComponents(source, filter);
            };
        };

        IndexWriterConfig conf = new IndexWriterConfig(analyzer);
        conf.setCodec(Codec.forName("Lucene50"));
        IndexWriter indexWriter = new IndexWriter(directory, conf);
       
        
        // Both fields f1 and f2 have 10 terms in total.
        // f1: the search terms (abc def) make 100% of all terms in f1
        // f2: the search terms (abc def) make 50% of all terms in f2
        // --> we expect that the sum of the boost factors for terms in bq(+f1:abc, +f1:def)
        // equals 2 * sum of the boost factors for terms in bq(+f2:abc, +f2:def)
        
        PRMSFieldBoostTest.addNumDocs("f1", "abc def", indexWriter, 2);
        PRMSFieldBoostTest.addNumDocs("f1", "abc", indexWriter, 4);
        PRMSFieldBoostTest.addNumDocs("f1", "def", indexWriter, 2);
        PRMSFieldBoostTest.addNumDocs("f2", "abc def", indexWriter, 1);
        PRMSFieldBoostTest.addNumDocs("f2", "abc", indexWriter, 2);
        PRMSFieldBoostTest.addNumDocs("f2", "def", indexWriter, 1);
        PRMSFieldBoostTest.addNumDocs("f2", "ghi", indexWriter, 5);
        
        indexWriter.close();
        
        IndexReader indexReader = DirectoryReader.open(directory); 
        IndexSearcher indexSearcher =  new IndexSearcher(indexReader);
        
        Map<String, Float> fields = new HashMap<>();
        fields.put("f1", 1f);
        fields.put("f2", 1f);
        SearchFieldsAndBoosting searchFieldsAndBoosting = new SearchFieldsAndBoosting(FieldBoostModel.PRMS, fields, fields, 0.8f);
        
        LuceneQueryBuilder queryBuilder = new LuceneQueryBuilder(dfc, analyzer, searchFieldsAndBoosting, 0.01f, null);
        
        WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
        
        Query query = queryBuilder.createQuery(parser.parse("AbcDef"));
        dfc.finishedUserQuery();
        query.createWeight(indexSearcher, true);
        
        assertTrue(query instanceof DisjunctionMaxQuery);
        
        DisjunctionMaxQuery dmq = (DisjunctionMaxQuery) query;
        ArrayList<Query> disjuncts = dmq.getDisjuncts();
        assertEquals(2, disjuncts.size());
        
        Query disjunct1 = disjuncts.get(0);
        assertTrue(disjunct1 instanceof BooleanQuery);
        
        BooleanQuery bq1 = (BooleanQuery) disjunct1;
        float sumBq1 = 0f;
        
        for (BooleanClause bc : bq1.clauses()) {
            assertEquals(Occur.MUST, bc.getOccur());
            Query clauseQuery = bc.getQuery();
            assertTrue(clauseQuery instanceof DependentTermQuery);
            sumBq1 += ((DependentTermQuery) clauseQuery).getFieldBoostFactor();
        }
        
        
        Query disjunct2 = disjuncts.get(1);
        assertTrue(disjunct2 instanceof BooleanQuery);
        
        BooleanQuery bq2 = (BooleanQuery) disjunct2;
        float sumBq2 = 0f;
        
        for (BooleanClause bc : bq2.clauses()) {
            assertEquals(Occur.MUST, bc.getOccur());
            Query clauseQuery = bc.getQuery();
            assertTrue(clauseQuery instanceof DependentTermQuery);
            sumBq2 += ((DependentTermQuery) clauseQuery).getFieldBoostFactor();
        }
        
        assertEquals(2f, sumBq1 / sumBq2, 0.00001);
        
        indexReader.close();
        directory.close();
        analyzer.close();    
        
    }

}
