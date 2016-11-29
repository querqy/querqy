package querqy.lucene.rewrite.prms;

import java.util.HashMap;
import java.util.List;
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
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.CharsRef;
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

public class PRMSDisjunctionMaxQueryTest extends LuceneTestCase {

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
        conf.setCodec(Codec.forName(TestUtil.LUCENE_CODEC));
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
        indexSearcher.setSimilarity(similarity);
        
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
        List<Query> disjuncts = dmq.getDisjuncts();
        assertEquals(2, disjuncts.size());
        
        Query disjunct1 = disjuncts.get(0);
        assertTrue(disjunct1 instanceof DisjunctionMaxQuery);
        Query dmq1 = disjunct1.rewrite(indexReader);
        if (dmq1 instanceof BoostQuery) {
            dmq1 = ((BoostQuery) dmq1).getQuery();
        }


        Query disjunct2 = disjuncts.get(1);
        assertTrue(disjunct2 instanceof DisjunctionMaxQuery);
        Query dmq2 = disjunct2.rewrite(indexReader);
        if (dmq2 instanceof BoostQuery) {
            dmq2 = ((BoostQuery) dmq2).getQuery();
        }


        final Weight weight1 = dmq1.createWeight(indexSearcher, true);
        weight1.normalize(0.1f, 4f);

        final Weight weight2 = dmq2.createWeight(indexSearcher, true);
        weight2.normalize(0.1f, 4f);
        
        Mockito.verify(simWeight, times(4)).normalize(eq(0.1f), normalizeCaptor.capture());
        final List<Float> capturedBoosts = normalizeCaptor.getAllValues();

        // capturedBoosts = boosts of [dmq1.term1, dmq1.term2, dmq2.term1, dmq2.term2 ]
        assertEquals(capturedBoosts.get(0), capturedBoosts.get(1), 0.00001);
        assertEquals(capturedBoosts.get(2), capturedBoosts.get(3), 0.00001);
        assertEquals(0.8f / 0.6f, capturedBoosts.get(0) / capturedBoosts.get(3), 0.00001);
        
        indexReader.close();
        directory.close();
        indexAnalyzer.close();    
        queryAnalyzer.close();    
        
    }

}
