package querqy.lucene.rewrite.prms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.tests.util.LuceneTestCase;
import org.junit.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import querqy.lucene.rewrite.DependentTermQueryBuilder;
import querqy.lucene.rewrite.DocumentFrequencyCorrection;
import querqy.lucene.rewrite.LuceneQueryBuilder;
import querqy.lucene.rewrite.SearchFieldsAndBoosting;
import querqy.lucene.rewrite.SearchFieldsAndBoosting.FieldBoostModel;
import querqy.lucene.rewrite.TestUtil;
import querqy.parser.WhiteSpaceQuerqyParser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PRMSAndQueryTest extends LuceneTestCase {

    @Test
    public void testGetThatFieldProbabilityRatioIsReflectedInBoost() throws Exception {

        DocumentFrequencyCorrection dfc = new DocumentFrequencyCorrection();

        Directory directory = newDirectory();
        
        Analyzer analyzer = new Analyzer() {
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new WhitespaceTokenizer();
                TokenStream filter = new WordDelimiterGraphFilter(source,
                        WordDelimiterGraphFilter.GENERATE_WORD_PARTS
                            | WordDelimiterGraphFilter.SPLIT_ON_CASE_CHANGE
                            , null);
                filter = new LowerCaseFilter(filter);
                return new TokenStreamComponents(source, filter);
            }
        };

        IndexWriterConfig conf = new IndexWriterConfig(analyzer);
        conf.setCodec(Codec.forName(TestUtil.LUCENE_CODEC));
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
        
        Map<String, Float> fields = new HashMap<>();
        fields.put("f1", 1f);
        fields.put("f2", 1f);
        SearchFieldsAndBoosting searchFieldsAndBoosting = new SearchFieldsAndBoosting(FieldBoostModel.PRMS, fields,
                fields, 0.8f);
        
        LuceneQueryBuilder queryBuilder = new LuceneQueryBuilder(new DependentTermQueryBuilder(dfc), analyzer,
                searchFieldsAndBoosting, 0.01f, 1f, null, null);
        
        WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
        
        Query query = queryBuilder.createQuery(parser.parse("AbcDef"));
        dfc.finishedUserQuery();

        assertTrue(query instanceof DisjunctionMaxQuery);
        
        DisjunctionMaxQuery dmq = (DisjunctionMaxQuery) query;
        List<Query> disjuncts = new ArrayList<>(dmq.getDisjuncts());
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

        Similarity similarity = mock(Similarity.class);
        Similarity.SimScorer simScorer = mock(Similarity.SimScorer.class);

        ArgumentCaptor<Float> computeWeightBoostCaptor = ArgumentCaptor.forClass(Float.class);

        when(similarity.scorer(
                computeWeightBoostCaptor.capture(),
                any(CollectionStatistics.class),
                ArgumentMatchers.<TermStatistics>any())).thenReturn(simScorer);

        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher =  new IndexSearcher(indexReader);
        indexSearcher.setSimilarity(similarity);

        Weight weight1 = indexSearcher.createWeight(bq1, ScoreMode.COMPLETE, 1.0f);
        Weight weight2 = indexSearcher.createWeight(bq2, ScoreMode.COMPLETE, 1.0f);

        final List<Float> capturedBoosts = computeWeightBoostCaptor.getAllValues();

        // capturedBoosts = boosts of [bq1.term1, bq1.term2, bq2.term1, bq2.term2 ]
        assertEquals(capturedBoosts.get(0), capturedBoosts.get(1), 0.00001);
        assertEquals(capturedBoosts.get(2), capturedBoosts.get(3), 0.00001);

        final Query query1 = bq1.clauses().get(0).query();
        assertTrue(query1 instanceof DependentTermQueryBuilder.DependentTermQuery);
        final String field1 = ((DependentTermQueryBuilder.DependentTermQuery) query1).getTerm().field();
        // Dismax clauses are a set - we have no guarantee about order -> identify by field name

        assertEquals("f1".equals(field1) ? 2f : 0.5f, capturedBoosts.get(0) / capturedBoosts.get(3), 0.00001);

        indexReader.close();
        directory.close();
        analyzer.close();    
        
    }

}
