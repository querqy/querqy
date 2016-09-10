package querqy.lucene.rewrite.prms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Test;

import querqy.lucene.rewrite.DependentTermQuery;
import querqy.lucene.rewrite.DocumentFrequencyCorrection;
import querqy.lucene.rewrite.LuceneQueryBuilder;
import querqy.lucene.rewrite.SearchFieldsAndBoosting;
import querqy.lucene.rewrite.SearchFieldsAndBoosting.FieldBoostModel;
import querqy.parser.WhiteSpaceQuerqyParser;

public class PRMSDisjunctionMaxQueryTest extends LuceneTestCase {

    @Test
    public void testGetThatFieldProbabilityRatioIsReflectedInBoost() throws Exception {
        
        DocumentFrequencyCorrection dfc = new DocumentFrequencyCorrection();

        Directory directory = newDirectory();
        
        SynonymMap.Builder builder = new SynonymMap.Builder(true);
        builder.add(new CharsRef("abc"), new CharsRef("def"), true);
        final SynonymMap synonyms = builder.build();
        
        Analyzer queryAnalyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                WhitespaceTokenizer source = new WhitespaceTokenizer();
                TokenStream result = new SynonymFilter(source, synonyms, true);
                return new TokenStreamComponents(source, result);
            }
        };
        
        Analyzer indexAnalyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                WhitespaceTokenizer source = new WhitespaceTokenizer();
                return new TokenStreamComponents(source, source);
            }
        };

        IndexWriterConfig conf = new IndexWriterConfig(indexAnalyzer);
        conf.setCodec(Codec.forName("Lucene50"));
        IndexWriter indexWriter = new IndexWriter(directory, conf);
       
        PRMSFieldBoostTest.addNumDocs("f1", "abc", indexWriter, 2);
        PRMSFieldBoostTest.addNumDocs("f1", "def", indexWriter, 8); 
        PRMSFieldBoostTest.addNumDocs("f2", "abc", indexWriter, 6); 
        PRMSFieldBoostTest.addNumDocs("f2", "def", indexWriter, 4); 
        
//         Within a field, all disjuncts must have the same boost factor, reflecting
//         the max boost factor of the disjuncts.
//         Given a query 'abc' and synonym expansion 'def', we get a query:
//         DMQ(                              
//            DMQ(                           // dmq1
//               TQ(f1:abc),
//               TQ(f1:def)
//               ),
//            DMQ(                           // dmq2
//               TQ(f2:abc),
//               TQ(f2:def)
//               ),
//         )
//         dmq1: max boost factor is 0.8 (8 of 10 terms in f1 equal "abc")  
//         dmq2: max boost factor is 0.6 (6 of 10 terms in f2 equal "def") 
//         ==> the ratio of the boost factors of the disjuncts of dmq1/dmq2 must equal 0.8/0.6         
        
        
        indexWriter.close();
        
        IndexReader indexReader = DirectoryReader.open(directory); 
        IndexSearcher indexSearcher =  new IndexSearcher(indexReader);
        
        Map<String, Float> fields = new HashMap<>();
        fields.put("f1", 1f);
        fields.put("f2", 1f);
        SearchFieldsAndBoosting searchFieldsAndBoosting = new SearchFieldsAndBoosting(FieldBoostModel.PRMS, fields, fields, 0.8f);
        
        LuceneQueryBuilder queryBuilder = new LuceneQueryBuilder(dfc, queryAnalyzer, searchFieldsAndBoosting, 0.01f, null);
        
        WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
        
        Query query = queryBuilder.createQuery(parser.parse("abc"));
        dfc.finishedUserQuery();
        query.createWeight(indexSearcher, true);
        
        assertTrue(query instanceof DisjunctionMaxQuery);
        
        DisjunctionMaxQuery dmq = (DisjunctionMaxQuery) query;
        ArrayList<Query> disjuncts = dmq.getDisjuncts();
        assertEquals(2, disjuncts.size());
        
        Query disjunct1 = disjuncts.get(0);
        assertTrue(disjunct1 instanceof DisjunctionMaxQuery);
        DisjunctionMaxQuery dmq1 = (DisjunctionMaxQuery) disjunct1;
        
        Query disjunct2 = disjuncts.get(1);
        assertTrue(disjunct2 instanceof DisjunctionMaxQuery);
        DisjunctionMaxQuery dmq2 = (DisjunctionMaxQuery) disjunct2;
        
        Float bf1 = null;
        for (Query disjunct: dmq1.getDisjuncts()) {
            assertTrue(disjunct instanceof DependentTermQuery);
            if (bf1 == null) {
                bf1 = ((DependentTermQuery) disjunct).getFieldBoostFactor();
            } else {
                assertEquals(bf1,  ((DependentTermQuery) disjunct).getFieldBoostFactor());
            }
        }
        
        Float bf2 = null;
        for (Query disjunct: dmq2.getDisjuncts()) {
            assertTrue(disjunct instanceof DependentTermQuery);
            if (bf2 == null) {
                bf2 = ((DependentTermQuery) disjunct).getFieldBoostFactor();
            } else {
                assertEquals(bf2,  ((DependentTermQuery) disjunct).getFieldBoostFactor());
            }
        }
        
        assertEquals(0.8/0.6f, bf1 / bf2, 0.00001);
        
        indexReader.close();
        directory.close();
        indexAnalyzer.close();    
        queryAnalyzer.close();    
        
    }

}
