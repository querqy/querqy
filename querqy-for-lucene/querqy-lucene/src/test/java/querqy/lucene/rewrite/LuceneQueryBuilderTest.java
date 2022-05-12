package querqy.lucene.rewrite;

import static org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.GENERATE_NUMBER_PARTS;
import static org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.PRESERVE_ORIGINAL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import querqy.lucene.rewrite.SearchFieldsAndBoosting.FieldBoostModel;
import querqy.model.BooleanQuery;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.QuerqyQuery;
import querqy.model.Term;
import querqy.parser.FieldAwareWhiteSpaceQuerqyParser;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.ContextAwareQueryRewriter;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.SimpleCommonRulesRewriterFactory;
import querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory;
import querqy.rewrite.commonrules.select.SelectionStrategyFactory;

@RunWith(MockitoJUnitRunner.class)
public class LuceneQueryBuilderTest extends AbstractLuceneQueryTest {

    Analyzer analyzer;
    Map<String, Float> searchFields;
    Set<String> stopWords;

    @Mock
    SearchEngineRequestAdapter searchEngineRequestAdapter;

    @Before
    public void setUp() {
        analyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                WhitespaceTokenizer source = new WhitespaceTokenizer();
                TokenStream result = new WordDelimiterGraphFilter(source, GENERATE_NUMBER_PARTS | PRESERVE_ORIGINAL,
                        null);
                return new TokenStreamComponents(source, result);
            }
        };
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
                new DocumentFrequencyCorrection()), analyzer, searchFieldsAndBoosting, tie, 1f, null);

        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
        querqy.model.Query q = parser.parse(input);
        return builder.createQuery(q);
    }

    protected Query buildWithSynonyms(final String input, final float tie, final String... names) throws IOException {
        return buildWithSynonyms(input, tie, 1f, names);
    }

    protected Query buildWithSynonyms(final String input, final float tie, final float multiMatchTie,
                                      final String... names) throws IOException {
        Map<String, Float> fields = fields(names);
       
        SearchFieldsAndBoosting searchFieldsAndBoosting = new SearchFieldsAndBoosting(FieldBoostModel.FIXED, fields, fields, 0.8f);
       
        LuceneQueryBuilder builder = new LuceneQueryBuilder(new DependentTermQueryBuilder(
                new DocumentFrequencyCorrection()), analyzer, searchFieldsAndBoosting, tie, multiMatchTie, null);

        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
        querqy.model.Query q = parser.parse(input);

        SimpleCommonRulesRewriterFactory factory = new SimpleCommonRulesRewriterFactory("CommonRulesRewriter",
                new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                        getClass().getClassLoader().getResourceAsStream("rules-synonyms.txt")),
                        StandardCharsets.UTF_8)), true,
                new WhiteSpaceQuerqyParserFactory(), true, Collections.emptyMap(),
                (rewriterId, searchEngineRequestAdapter) -> SelectionStrategyFactory.DEFAULT_SELECTION_STRATEGY,
                true);

        ContextAwareQueryRewriter rewriter = (ContextAwareQueryRewriter) factory.createRewriter(null,
                searchEngineRequestAdapter);


        final QuerqyQuery<?> userQuery = rewriter.rewrite(new ExpandedQuery(q), searchEngineRequestAdapter).getUserQuery();
        return builder.createQuery(userQuery);

    }
   
    protected Query buildWithStopWords(String input, float tie, String... names) {
        Map<String, Float> fields = fields(names);
       
        SearchFieldsAndBoosting searchFieldsAndBoosting = new SearchFieldsAndBoosting(FieldBoostModel.FIXED, fields,
                fields, 0.8f);
       
        LuceneQueryBuilder builder = new LuceneQueryBuilder(new DependentTermQueryBuilder(
                new DocumentFrequencyCorrection()), new StandardAnalyzer(new CharArraySet(stopWords, true)),
                searchFieldsAndBoosting, tie, 1f, null);

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
            dtq(0.2f, "f1", "q")
            ));
   }

    @Test
    public void testMultiMatchNotApplied() throws Exception {
        float tie = (float) Math.random();
        final float multiMatchTie = 1f; // do not rewrite for 1f
        Query q = buildWithSynonyms("nn j", tie, multiMatchTie, "f1", "f2");
        assertThat(q,
                bq(
                        dmq(BooleanClause.Occur.SHOULD, 1f, tie,
                                dtq(1f, "f1", "nn"),
                                dtq(2f, "f2", "nn")

                        ),
                        dmq(BooleanClause.Occur.SHOULD, 1f, tie,
                                dtq(1f, "f1", "j"),
                                dtq(2f, "f2", "j"),
                                bq(0.5f,
                                        dmq(BooleanClause.Occur.MUST, 1f, tie,
                                                dtq(1f, "f1", "s"),
                                                dtq(2f, "f2", "s")
                                        ),
                                        dmq(BooleanClause.Occur.MUST, 1f, tie,
                                                dtq(1f, "f1", "t"),
                                                dtq(2f, "f2", "t")
                                        )
                                ),
                                dtq(0.2f, "f1", "q"), // synonym weight is 0.2
                                dtq(0.2f*2f, "f2", "q") // synonym = 0.2, field weight = 2
                        )
                )
        );
    }

    @Test
    public void testMultiMatch01() throws Exception {
        float tie = 0.7f;
        final float multiMatchTie = 0.6f;
        Query q = buildWithSynonyms("j", tie, multiMatchTie, "f1", "f2");
        assertThat(q,
                dmq(1f, tie,
                        dmq(1f, multiMatchTie,
                                dtq(1f, "f1", "j"),
                                dtq(0.2f, "f1", "q"), // synonym weight is 0.2
                                bq(0.5f,
                                        dmq(BooleanClause.Occur.MUST, 1f, tie,
                                                dtq(1f, "f1", "s"),
                                                dtq(0f, "f2", "s")
                                        ),
                                        dmq(BooleanClause.Occur.MUST, 1f, tie,
                                                dtq(1f, "f1", "t"),
                                                dtq(0f, "f2", "t")
                                        )
                                )


                        ),
                        dmq(1f, multiMatchTie,
                                dtq(2f, "f2", "j"),
                                dtq(0.2f*2f, "f2", "q"), // synonym = 0.2, field weight = 2
                                bq(0.5f,
                                        dmq(BooleanClause.Occur.MUST, 1f, tie,
                                                dtq(0f, "f1", "s"),
                                                dtq(2f, "f2", "s")
                                        ),
                                        dmq(BooleanClause.Occur.MUST, 1f, tie,
                                                dtq(0f, "f1", "t"),
                                                dtq(2f, "f2", "t")
                                        )
                                )

                        )
                )
        );
    }

    @Test
    public void testMultiMatch02() throws Exception {
        float tie = 0.7f;
        final float multiMatchTie = 0.6f;
        Query q = buildWithSynonyms("nn j", tie, multiMatchTie, "f1", "f2");
        assertThat(q,
                bq(
                        dmq(BooleanClause.Occur.SHOULD, 1f, tie,
                                dtq(1f, "f1", "nn"),
                                dtq(2f, "f2", "nn")

                        ),
                        dmq(BooleanClause.Occur.SHOULD, 1f, tie,
                                dmq(1f, multiMatchTie,
                                    dtq(1f, "f1", "j"),
                                    dtq(0.2f, "f1", "q"), // synonym weight is 0.2
                                    bq(0.5f,
                                        dmq(BooleanClause.Occur.MUST, 1f, tie,
                                                dtq(1f, "f1", "s"),
                                                dtq(0f, "f2", "s")
                                        ),
                                        dmq(BooleanClause.Occur.MUST, 1f, tie,
                                                dtq(1f, "f1", "t"),
                                                dtq(0f, "f2", "t")
                                        )
                                    )


                                ),
                                dmq(1f, multiMatchTie,
                                        dtq(2f, "f2", "j"),
                                        dtq(0.2f*2f, "f2", "q"), // synonym = 0.2, field weight = 2
                                        bq(0.5f,
                                                dmq(BooleanClause.Occur.MUST, 1f, tie,
                                                        dtq(0f, "f1", "s"),
                                                        dtq(2f, "f2", "s")
                                                ),
                                                dmq(BooleanClause.Occur.MUST, 1f, tie,
                                                        dtq(0f, "f1", "t"),
                                                        dtq(2f, "f2", "t")
                                                )
                                        )

                                )
                    )
                )
        );
    }

    @Test
    public void testMultiMatch03() throws Exception {
        final float multiMatchTie = 0.6f;
        final float tie = 0.8f;
        Query q = buildWithSynonyms("100-2 j", tie, multiMatchTie, "f1", "f2");
        assertThat(q,
                bq(
                        //clause 1
                        dmq(BooleanClause.Occur.SHOULD, 1f, tie,

                                // f1:
                                dmq(1f, multiMatchTie,
                                        dtq(1f, "f1", "onehundreddashtwo"),
                                        dmq(1f, 0f,
                                            dtq(1f, "f1", "100-2"),
                                            bq(0.5f,
                                                    dtq(BooleanClause.Occur.MUST, 1f, "f1", "100"),
                                                    dtq(BooleanClause.Occur.MUST, 1f, "f1", "2")
                                            )
                                        )

                                ),
                                // f2:
                                dmq(1f, multiMatchTie,
                                        dtq(2f, "f2", "onehundreddashtwo"),
                                        dmq(1f, 0f,
                                            dtq(2f, "f2", "100-2"),
                                            bq(0.5f,
                                                    dtq(BooleanClause.Occur.MUST, 2f, "f2", "100"),
                                                    dtq(BooleanClause.Occur.MUST, 2f, "f2", "2")

                                            )
                                        )

                                )
                        )
                        , //clause 2
                        dmq(BooleanClause.Occur.SHOULD, 1f, tie,
                                dmq(1f, multiMatchTie,
                                        dtq(1f, "f1", "j"),
                                        bq(0.5f,
                                                dmq(BooleanClause.Occur.MUST, 1f, tie,
                                                        dtq(1f, "f1", "s"),
                                                        dtq(0f, "f2", "s")
                                                ),
                                                dmq(BooleanClause.Occur.MUST, 1f, tie,
                                                        dtq(1f, "f1", "t"),
                                                        dtq(0f, "f2", "t")
                                                )
                                        ),
                                        dtq(0.2f, "f1", "q") // synonym weight is 0.2

                                ),
                                dmq(1f, multiMatchTie,
                                        dtq(2f, "f2", "j"),
                                        bq(0.5f,
                                                dmq(BooleanClause.Occur.MUST, 1f, tie,
                                                        dtq(0f, "f1", "s"),
                                                        dtq(2f, "f2", "s")
                                                ),
                                                dmq(BooleanClause.Occur.MUST, 1f, tie,
                                                        dtq(0f, "f1", "t"),
                                                        dtq(2f, "f2", "t")
                                                )
                                        ),
                                        dtq(0.2f*2f, "f2", "q") // synonym = 0.2, field weight = 2

                                )
                        )
                )
        );
    }

    @Test
    public void testMultiMatchWithSingleTermSynonym() throws Exception {
        final float multiMatchTie = 0.6f;
        final float tie = 0.8f;
        Query q = buildWithSynonyms("abc", tie, multiMatchTie, "f1", "f2");
        assertThat(q,
                dmq(1f, tie,
                        dmq( 1f, multiMatchTie,
                                dtq(1f, "f1", "abc"),
                                dtq(1f, "f1", "def")

                        ),
                        dmq( 1f, multiMatchTie,
                                dtq(2f, "f2", "abc"),
                                dtq(2f, "f2", "def")

                        )
                )
        );

    }

    @Test
    public void testMultiMatchWithSingleToMultiTermSynonym() throws Exception {
        final float multiMatchTie = 0.6f;
        final float tie = 0.8f;
        Query q = buildWithSynonyms("f", tie, multiMatchTie, "f1", "f2");
        assertThat(q,
                dmq(1f, tie,
                        // field 1
                        dmq(1f, multiMatchTie,
                                dtq(1f, "f1", "f"),
                                bq(0.5f,
                                        dmq(BooleanClause.Occur.MUST, 1f, tie,
                                                dtq(1f, "f1", "k"),
                                                dtq(0f, "f2", "k")
                                        ),
                                        dmq(BooleanClause.Occur.MUST, 1f, tie,
                                                dtq(1f, "f1", "l"),
                                                dtq(0f, "f2", "l")
                                        )
                                )
                        ),
                        // field 2
                        dmq(1f, multiMatchTie,
                                dtq(2f, "f2", "f"),
                                bq(0.5f,
                                        dmq(BooleanClause.Occur.MUST, 1f, tie,
                                                dtq(0f, "f1", "k"),
                                                dtq(2f, "f2", "k")
                                        ),
                                        dmq(BooleanClause.Occur.MUST, 1f, tie,
                                                dtq(0f, "f1", "l"),
                                                dtq(2f, "f2", "l")
                                        )
                                )
                        )
                )
        );

    }

    @Test
    public void testNestedMultiMatch() {
        BooleanQuery bq0 = new BooleanQuery(null, Clause.Occur.MUST, false);

        // level 1
        DisjunctionMaxQuery dmq1a = new DisjunctionMaxQuery(bq0, Clause.Occur.MUST, false);
        bq0.addClause(dmq1a);
        dmq1a.addClause(new Term(dmq1a, "term1a"));
        dmq1a.addClause(new Term(dmq1a, "term1b"));

        BooleanQuery bq1 = new BooleanQuery(bq0, Clause.Occur.MUST, false);
        bq0.addClause(bq1);

        // level 2
        DisjunctionMaxQuery dmq2a = new DisjunctionMaxQuery(bq1, Clause.Occur.MUST, false);
        bq1.addClause(dmq2a);
        dmq2a.addClause(new Term(dmq2a, "term2a"));
        dmq2a.addClause(new Term(dmq2a, "term2b"));

        DisjunctionMaxQuery dmq2b = new DisjunctionMaxQuery(bq1, Clause.Occur.MUST, false);
        bq1.addClause(dmq2b);
        dmq2b.addClause(new Term(dmq2b, "term2c"));
        dmq2b.addClause(new Term(dmq2b, "term2d"));

        // level 1
        DisjunctionMaxQuery dmq1b = new DisjunctionMaxQuery(bq0, Clause.Occur.MUST, false);
        bq0.addClause(dmq1b);
        dmq1b.addClause(new Term(dmq1b, "term1c"));
        dmq1b.addClause(new Term(dmq1b, "term1d"));

        Map<String, Float> fieldsQuery = new HashMap<>();
        fieldsQuery.put("f1", 1f);
        fieldsQuery.put("f2", 2f);

        SearchFieldsAndBoosting searchFieldsAndBoosting = new SearchFieldsAndBoosting(FieldBoostModel.FIXED,
                fieldsQuery, Collections.emptyMap(), 0.9f);
        final float multiMatchTie = 0.6f;
        final float tie = 0.8f;

        LuceneQueryBuilder builder = new LuceneQueryBuilder(new DependentTermQueryBuilder(
                new DocumentFrequencyCorrection()), analyzer, searchFieldsAndBoosting, tie, multiMatchTie, null);

        assertThat(builder.createQuery(bq0),
                bq(
                        // clause 1:
                        dmq(BooleanClause.Occur.MUST, 1f, tie,
                                dmq( 1f, multiMatchTie,
                                        dtq(1f, "f1", "term1a"),
                                        dtq(1f, "f1", "term1b")


                                ),
                                dmq( 1f, multiMatchTie,
                                        dtq(2f, "f2", "term1a"),
                                        dtq(2f, "f2", "term1b")

                                )
                        ),
                        // clause 2:
                        bq(BooleanClause.Occur.MUST,
                                // clause 2.1
                                dmq(BooleanClause.Occur.MUST, 1f, tie,
                                        // f1
                                        dmq( 1f, multiMatchTie,
                                                dtq(1f, "f1", "term2a"),
                                                dtq(1f, "f1", "term2b")

                                        ),
                                        // f2
                                        dmq( 1f, multiMatchTie,
                                                dtq(2f, "f2", "term2b"),
                                                dtq(2f, "f2", "term2a")
                                        )
                                ),
                                // clause 2.2
                                dmq(BooleanClause.Occur.MUST, 1f, tie,

                                        dmq( 1f, multiMatchTie,
                                                dtq(1f, "f1", "term2c"),
                                                dtq(1f, "f1", "term2d")

                                        ),
                                        dmq( 1f, multiMatchTie,
                                                dtq(2f, "f2", "term2c"),
                                                dtq(2f, "f2", "term2d")
                                        )
                                )
                        ),
                        // clause 3:
                        dmq(BooleanClause.Occur.MUST, 1f, tie,
                                dmq( 1f, multiMatchTie,
                                        dtq(1f, "f1", "term1c"),
                                        dtq(1f, "f1", "term1d")
                                ),
                                dmq( 1f, multiMatchTie,
                                        dtq(2f, "f2", "term1c"),
                                        dtq(2f, "f2", "term1d")
                                )
                        )


                )
        );


    }

    @Test
    public void testPurelyNegativeQueriesFromWhitespaceQuerqyParser() throws IOException {

        float tie = (float) Math.random();

        Map<String, Float> fields = fields("f1", "f2");

        SearchFieldsAndBoosting searchFieldsAndBoosting = new SearchFieldsAndBoosting(FieldBoostModel.FIXED, fields, fields, 0.8f);


        LuceneQueryBuilder builder = new LuceneQueryBuilder(new DependentTermQueryBuilder(new DocumentFrequencyCorrection()),
                analyzer, searchFieldsAndBoosting, tie, 1f, null);

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
               analyzer, searchFieldsAndBoosting, 0.1f, 1f, null);

       WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
       querqy.model.Query q = parser.parse("a");

       SimpleCommonRulesRewriterFactory factory = new SimpleCommonRulesRewriterFactory("CommonRulesRewriter",
               new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                       getClass().getClassLoader().getResourceAsStream("rules-synonyms.txt")),
                       StandardCharsets.UTF_8)), true,
               new WhiteSpaceQuerqyParserFactory(), true, Collections.emptyMap(),
               (rewriterId, searchEngineRequestAdapter) -> SelectionStrategyFactory.DEFAULT_SELECTION_STRATEGY,
               true);

       ContextAwareQueryRewriter rewriter = (ContextAwareQueryRewriter) factory.createRewriter(null,
               searchEngineRequestAdapter);

       Query query = builder.createQuery(rewriter.rewrite(new ExpandedQuery(q), searchEngineRequestAdapter).getUserQuery());
       
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
