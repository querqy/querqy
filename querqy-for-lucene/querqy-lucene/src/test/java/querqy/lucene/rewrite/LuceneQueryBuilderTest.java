package querqy.lucene.rewrite;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import querqy.lucene.contrib.rewrite.LuceneSynonymsRewriterFactory;
import querqy.lucene.rewrite.SearchFieldsAndBoosting.FieldBoostModel;
import querqy.model.ExpandedQuery;
import querqy.parser.FieldAwareWhiteSpaceQuerqyParser;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.SearchEngineRequestAdapter;

@RunWith(MockitoJUnitRunner.class)
public class LuceneQueryBuilderTest extends AbstractLuceneQueryTest {

    Analyzer keywordAnalyzer;
    Map<String, Float> searchFields;
    Set<String> stopWords;

    @Mock
    SearchEngineRequestAdapter searchEngineRequestAdapter;

    @Before
    public void setUp() {
        keywordAnalyzer = new KeywordAnalyzer();
        searchFields = new HashMap<>();
        searchFields.put("f1", 1.0f);
        searchFields.put("f11", 1.0f);
        searchFields.put("f12", 1.0f);
        searchFields.put("f13", 1.0f);
        searchFields.put("f14", 1.0f);
        searchFields.put("f15", 1.0f);

        searchFields.put("f2", 2.0f);
        searchFields.put("f21", 2.0f);
        searchFields.put("f22", 2.0f);
        searchFields.put("f23", 2.0f);
        searchFields.put("f24", 2.0f);
        searchFields.put("f25", 2.0f);

        searchFields.put("f3", 3.0f);
        searchFields.put("f31", 3.0f);
        searchFields.put("f32", 3.0f);
        searchFields.put("f33", 3.0f);
        searchFields.put("f34", 3.0f);
        searchFields.put("f35", 3.0f);

        stopWords = new HashSet<>(Arrays.asList("stopA", "stopB", "stopC"));

    }

    Map<String, Float> fields(String... names) {
        Map<String, Float> result = new HashMap<>(names.length);
        for (String name : names) {
            Float value = searchFields.get(name);
            if (value == null) {
                throw new IllegalArgumentException("No such field: " + name);
            }
            result.put(name, value);
        }
        return result;
    }

    protected Query build(String input, float tie, String... names) {
        Map<String, Float> fields = fields(names);
       
        SearchFieldsAndBoosting searchFieldsAndBoosting = new SearchFieldsAndBoosting(FieldBoostModel.FIXED, fields, fields, 0.8f);
       
        LuceneQueryBuilder builder = new LuceneQueryBuilder(new DependentTermQueryBuilder(
                new DocumentFrequencyCorrection()), keywordAnalyzer, searchFieldsAndBoosting, tie, null);

        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
        querqy.model.Query q = parser.parse(input);
        return builder.createQuery(q);
    }

    protected Query buildWithSynonyms(String input, float tie, String... names) throws IOException {
        Map<String, Float> fields = fields(names);
       
        SearchFieldsAndBoosting searchFieldsAndBoosting = new SearchFieldsAndBoosting(FieldBoostModel.FIXED, fields, fields, 0.8f);
       
        LuceneQueryBuilder builder = new LuceneQueryBuilder(new DependentTermQueryBuilder(
                new DocumentFrequencyCorrection()), keywordAnalyzer, searchFieldsAndBoosting, tie, null);

        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
        querqy.model.Query q = parser.parse(input);
        LuceneSynonymsRewriterFactory factory = new LuceneSynonymsRewriterFactory("LuceneSynonymsRewriter", true, true);
        factory.addResource(getClass().getClassLoader().getResourceAsStream("synonyms-test.txt"));
        factory.build();

        QueryRewriter rewriter = factory.createRewriter(null, searchEngineRequestAdapter);

        return builder.createQuery(rewriter.rewrite(new ExpandedQuery(q)).getUserQuery());

    }
   
    protected Query buildWithStopWords(String input, float tie, String... names) {
        Map<String, Float> fields = fields(names);
       
        SearchFieldsAndBoosting searchFieldsAndBoosting = new SearchFieldsAndBoosting(FieldBoostModel.FIXED, fields,
                fields, 0.8f);
       
        LuceneQueryBuilder builder = new LuceneQueryBuilder(new DependentTermQueryBuilder(
                new DocumentFrequencyCorrection()), new StandardAnalyzer(new CharArraySet(stopWords, true)),
                searchFieldsAndBoosting, tie, null);

        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
        querqy.model.Query q = parser.parse(input);
        return builder.createQuery(q);
    }

   @Test
   public void test01() throws IOException {
      float tie = (float) Math.random();
      Query q = build("a", tie, "f1");
      assertThat(q, dtq(1f, "f1", "a"));
   }

   @Test
   public void test02() throws IOException {
      float tie = (float) Math.random();
      Query q = build("a", tie, "f1", "f2");
      assertThat(q, dmq(1f, tie,
            dtq(1f, "f1", "a"),
            dtq(2f, "f2", "a")
            ));
   }

   @Test
   public void test03() throws Exception {
      float tie = (float) Math.random();
      Query q = build("a b", tie, "f1");
      assertThat(q, bq(1f,
            dtq(Occur.SHOULD, 1f, "f1", "a"),
            dtq(Occur.SHOULD, 1f, "f1", "b")
            ));
   }

   @Test
   public void test04() throws Exception {
      float tie = (float) Math.random();
      Query q = build("a +b", tie, "f1");
      assertThat(q, bq(1f,
            dtq(Occur.SHOULD, 1f, "f1", "a"),
            dtq(Occur.MUST, 1f, "f1", "b")
            ));
   }

   @Test
   public void test05() throws Exception {
      float tie = (float) Math.random();
      Query q = build("-a +b", tie, "f1");
      assertThat(q, bq(1f,
            dtq(Occur.MUST_NOT, 1f, "f1", "a"),
            dtq(Occur.MUST, 1f, "f1", "b")
            ));
   }

   @Test
   public void test06() throws Exception {
      float tie = (float) Math.random();
      Query q = build("a b", tie, "f1", "f2");
      assertThat(q, bq(1f,
            dmq(Occur.SHOULD, 1f, tie,
                  dtq(1f, "f1", "a"),
                  dtq(2f, "f2", "a")
            ),
            dmq(Occur.SHOULD, 1f, tie,
                  dtq(1f, "f1", "b"),
                  dtq(2f, "f2", "b")
            )
            ));
   }

   @Test
   public void test07() throws Exception {
      float tie = (float) Math.random();
      Query q = build("+a b", tie, "f1", "f2");
      assertThat(q, bq(1f,
            dmq(Occur.MUST, 1f, tie,
                  dtq(1f, "f1", "a"),
                  dtq(2f, "f2", "a")
            ),
            dmq(Occur.SHOULD, 1f, tie,
                  dtq(1f, "f1", "b"),
                  dtq(2f, "f2", "b")
            )
            ));
   }

   @Test
   public void test08() throws Exception {
      float tie = (float) Math.random();
      Query q = build("+a -b", tie, "f1", "f2");
      assertThat(q, bq(1f,
            dmq(Occur.MUST, 1f, tie,
                  dtq(1f, "f1", "a"),
                  dtq(2f, "f2", "a")
            ),
            bq(Occur.MUST_NOT, 1f,
                  dtq(Occur.SHOULD, 1f, "f1", "b"),
                  dtq(Occur.SHOULD, 2f, "f2", "b")
            )
            ));
   }

   @Test
   public void test09() throws Exception {
      float tie = (float) Math.random();
      Query q = build("a -b", tie, "f1", "f2");
      assertThat(q, bq(1f,
            dmq(Occur.SHOULD, 1f, tie,
                  dtq(1f, "f1", "a"),
                  dtq(2f, "f2", "a")
            ),
            bq(Occur.MUST_NOT, 1f,
                  dtq(Occur.SHOULD, 1f, "f1", "b"),
                  dtq(Occur.SHOULD, 2f, "f2", "b")
            )
            ));
   }

   @Test
   public void test10() throws Exception {
      float tie = (float) Math.random();
      Query q = build("-a -b c", tie, "f1", "f2");
      assertThat(q, bq(1f,
            dmq(Occur.SHOULD, 1f, tie,
                  dtq(1f, "f1", "c"),
                  dtq(2f, "f2", "c")
            ),
            bq(Occur.MUST_NOT, 1f,
                  dtq(Occur.SHOULD, 1f, "f1", "a"),
                  dtq(Occur.SHOULD, 2f, "f2", "a")
            ),
            bq(Occur.MUST_NOT, 1f,
                  dtq(Occur.SHOULD, 1f, "f1", "b"),
                  dtq(Occur.SHOULD, 2f, "f2", "b")
            )

            ));
   }

   @Test
   public void test11() throws Exception {
      float tie = (float) Math.random();
      Query q = build("f2:a", tie, "f1", "f2");
      assertThat(q, dtq(2f, "f2", "a"));
   }

   @Test
   public void test12() throws Exception {
      float tie = (float) Math.random();
      // query contains a field that is not contained in the search fields
      Query q = build("x2:a", tie, "f1", "f2");

      assertThat(q, dmq(1f, tie,
            dtq(1f, "f1", "x2:a"),
            dtq(2f, "f2", "x2:a")
            ));

   }

   @Test
   public void test13() throws Exception {
      float tie = (float) Math.random();
      Query q = buildWithSynonyms("j", tie, "f1");
      assertThat(q, dmq(1f, tie,
            dtq(1f, "f1", "j"),
            bq(0.5f,
                  dtq(Occur.MUST, 1f, "f1", "s"),
                  dtq(Occur.MUST, 1f, "f1", "t")
            ),
            dtq(1f, "f1", "q")
            ));
   }

    @Test
    public void testPurelyNegativeQueriesFromWhitespaceQuerqyParser() throws IOException {

        float tie = (float) Math.random();

        Map<String, Float> fields = fields("f1", "f2");

        SearchFieldsAndBoosting searchFieldsAndBoosting = new SearchFieldsAndBoosting(FieldBoostModel.FIXED, fields, fields, 0.8f);


        LuceneQueryBuilder builder = new LuceneQueryBuilder(new DependentTermQueryBuilder(new DocumentFrequencyCorrection()),
                keywordAnalyzer, searchFieldsAndBoosting, tie, null);

        Query q = builder.createQuery(new FieldAwareWhiteSpaceQuerqyParser().parse("-ab"));

        assertThat(q, bq(
                bq(Occur.MUST_NOT,
                        dtq(Occur.SHOULD, 1f, "f1", "ab"),
                        dtq(Occur.SHOULD, 2f, "f2", "ab")
                )
        ));
    }
   
   
   @Test
   public void testSynonymsWithRestrictedFieldsForGeneratedTerms() throws Exception {
       
       
       Map<String, Float> fieldsQuery = new HashMap<>();
       fieldsQuery.put("f1", 2f);
       fieldsQuery.put("f2", 3f);
       
       Map<String, Float> fieldsGenerated = new HashMap<>();
       fieldsGenerated.put("f1", 4f);
       
       SearchFieldsAndBoosting searchFieldsAndBoosting = new SearchFieldsAndBoosting(FieldBoostModel.FIXED, fieldsQuery, fieldsGenerated, 0.8f);
       
       LuceneQueryBuilder builder = new LuceneQueryBuilder(new DependentTermQueryBuilder(new DocumentFrequencyCorrection()),
            keywordAnalyzer, searchFieldsAndBoosting, 0.1f, null);

       WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
       querqy.model.Query q = parser.parse("a");
       LuceneSynonymsRewriterFactory factory = new LuceneSynonymsRewriterFactory("LuceneSynonymsRewriter", true, true);
       factory.addResource(getClass().getClassLoader().getResourceAsStream("synonyms-test.txt"));
       factory.build();

       QueryRewriter rewriter = factory.createRewriter(null, searchEngineRequestAdapter);

       Query query = builder.createQuery(rewriter.rewrite(new ExpandedQuery(q)).getUserQuery());
       
       assertThat(query, 
           dmq(1f, 0.1f,
                      dtq(2f, "f1", "a"),
                      dtq(3f, "f2", "a"),
                      dtq(4f, "f1", "x")
           ));
      
   }

   
   

   @Test
   public  void testStopWordRemoval() throws Exception {
       float tie = (float) Math.random();
       Query q = buildWithStopWords("a stopA b", tie, "f1");
       assertThat(q, bq(1f,
               dtq(Occur.SHOULD, 1f, "f1", "a"),
               dtq(Occur.SHOULD, 1f, "f1", "b")
               ));
   }
   
   @Test
   public  void testStopWordAsSingleTermRemoval() throws Exception {
       float tie = (float) Math.random();
       Query q = buildWithStopWords("stopA", tie, "f1", "f2");
       assertTrue(q instanceof MatchNoDocsQuery);
   }
   
   @Test
   public void testEqualityOfCreatedQueries() throws Exception {
       float tie = (float) Math.random();
       Query q1 = buildWithSynonyms("a b j c", tie, "f1", "f2");
       Query q2 = buildWithSynonyms("a b j c", tie, "f1", "f2");
       assertTrue(q1.equals(q2));
       assertEquals(q1.hashCode(), q2.hashCode());
   }
}
