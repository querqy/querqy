package querqy.lucene.rewrite.prms;

import java.util.HashMap;
import java.util.List;
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
import org.apache.lucene.search.*;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Before;
import org.junit.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import querqy.lucene.rewrite.DependentTermQuery;
import querqy.lucene.rewrite.DocumentFrequencyCorrection;
import querqy.lucene.rewrite.LuceneQueryBuilder;
import querqy.lucene.rewrite.SearchFieldsAndBoosting;
import querqy.lucene.rewrite.SearchFieldsAndBoosting.FieldBoostModel;
import querqy.parser.WhiteSpaceQuerqyParser;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;

public class PRMSAndQueryTest extends LuceneTestCase {

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
        
        
        Analyzer analyzer = new Analyzer() {
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new WhitespaceTokenizer();
                TokenStream filter = new WordDelimiterFilter(source,
                            WordDelimiterFilter.GENERATE_WORD_PARTS 
                            | WordDelimiterFilter.SPLIT_ON_CASE_CHANGE
                            , null);
                filter = new LowerCaseFilter(filter);
                return new TokenStreamComponents(source, filter);
            }
        };

        IndexWriterConfig conf = new IndexWriterConfig(analyzer);
        conf.setCodec(Codec.forName("Lucene60"));
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
        indexSearcher.setSimilarity(similarity);
        
        Map<String, Float> fields = new HashMap<>();
        fields.put("f1", 1f);
        fields.put("f2", 1f);
        SearchFieldsAndBoosting searchFieldsAndBoosting = new SearchFieldsAndBoosting(FieldBoostModel.PRMS, fields, fields, 0.8f);
        
        LuceneQueryBuilder queryBuilder = new LuceneQueryBuilder(dfc, analyzer, searchFieldsAndBoosting, 0.01f, null);
        
        WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
        
        Query query = queryBuilder.createQuery(parser.parse("AbcDef"));
        dfc.finishedUserQuery();

        assertTrue(query instanceof DisjunctionMaxQuery);
        
        DisjunctionMaxQuery dmq = (DisjunctionMaxQuery) query;
        List<Query> disjuncts = dmq.getDisjuncts();
        assertEquals(2, disjuncts.size());
        
        Query disjunct1 = disjuncts.get(0);
        if (disjunct1 instanceof BoostQuery) {
            disjunct1 = ((BoostQuery) disjunct1).getQuery();
        }
        assertTrue(disjunct1 instanceof BooleanQuery);
        
        BooleanQuery bq1 = (BooleanQuery) disjunct1;

        Query disjunct2 = disjuncts.get(1);
        if (disjunct2 instanceof BoostQuery) {
            disjunct2 = ((BoostQuery) disjunct2).getQuery();
        }
        assertTrue(disjunct2 instanceof BooleanQuery);

        BooleanQuery bq2 = (BooleanQuery) disjunct2;


        final Weight weight1 = bq1.createWeight(indexSearcher, true);
        weight1.normalize(0.1f, 4f);

        final Weight weight2 = bq2.createWeight(indexSearcher, true);
        weight2.normalize(0.1f, 4f);


        Mockito.verify(simWeight, times(4)).normalize(eq(0.1f), normalizeCaptor.capture());
        final List<Float> capturedBoosts = normalizeCaptor.getAllValues();

        // capturedBoosts = boosts of [bq1.term1, bq1.term2, bq2.term1, bq2.term2 ]
        assertEquals(capturedBoosts.get(0), capturedBoosts.get(1), 0.00001);
        assertEquals(capturedBoosts.get(2), capturedBoosts.get(3), 0.00001);
        assertEquals(2f, capturedBoosts.get(0) / capturedBoosts.get(3), 0.00001);

        indexReader.close();
        directory.close();
        analyzer.close();    
        
    }

}
