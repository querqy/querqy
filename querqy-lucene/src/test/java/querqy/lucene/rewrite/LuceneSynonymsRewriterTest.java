package querqy.lucene.rewrite;

import static org.hamcrest.MatcherAssert.assertThat;
import static querqy.QuerqyMatchers.*;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Before;
import org.junit.Test;

import querqy.antlr.QueryTransformerVisitor;
import querqy.antlr.parser.QueryLexer;
import querqy.antlr.parser.QueryParser;
import querqy.antlr.parser.QueryParser.QueryContext;
import querqy.model.Clause.Occur;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;

public class LuceneSynonymsRewriterTest {

   QueryRewriter rewriter;

   @Before
   public void setUp() throws Exception {

      LuceneSynonymsRewriterFactory factory = new LuceneSynonymsRewriterFactory(true, true);
      factory.addResource(getClass().getClassLoader().getResourceAsStream("synonyms-test.txt"));
      factory.build();

      rewriter = factory.createRewriter(null, null);
   }

   protected ExpandedQuery makeQuery(String input) {
      QueryLexer lex = new QueryLexer(new ANTLRInputStream(input));
      CommonTokenStream tokens = new CommonTokenStream(lex);
      QueryParser parser = new QueryParser(tokens);

      QueryContext t = parser.query();
      return new ExpandedQuery((Query) t.accept(new QueryTransformerVisitor(input.toCharArray())));
   }

   @Test
   public void testSingleClauseExpansion() {
      ExpandedQuery q = makeQuery("a");

      assertThat(rewriter.rewrite(q).getUserQuery(),
            bq(
            dmq(
                  term("a"),
                  term("x")
            )
            ));

   }

   @Test
   public void testThatGeneratedTermIsNotExpanded() throws Exception {
      Query query = new Query();

      DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Occur.SHOULD, false);
      query.addClause(dmq);

      Term term = new Term(dmq, "a", true);
      dmq.addClause(term);

      assertThat(rewriter.rewrite(new ExpandedQuery(query)).getUserQuery(),
            bq(
            dmq(
            term("a")
            )
            ));

   }

   @Test
   public void testSingleClauseExpansionWithMultiCharWords() {
      ExpandedQuery q = makeQuery("abc");

      assertThat(rewriter.rewrite(q).getUserQuery(),
            bq(
            dmq(
                  term("abc"),
                  term("def")
            )
            ));

   }

   @Test
   public void testSingleClauseInputToMultiClauseOutput() {
      ExpandedQuery q = makeQuery("f");

      assertThat(rewriter.rewrite(q).getUserQuery(),
            bq(
            dmq(
                  term("f"),
                  bq(
                        dmq(must(), term("k")),
                        dmq(must(), term("l"))
                  )
            )
            ));

   }

   @Test
   public void testThatPartialMatchDoesntGetExpanded() throws Exception {
      // abc => ...
      // is in the synonym dict
      ExpandedQuery q = makeQuery("ab c");

      assertThat(rewriter.rewrite(q).getUserQuery(),
            bq(
                  dmq(term("ab")),
                  dmq(term("c"))

            ));
   }

   @Test
   public void testSingleClauseInputToMixedOutput() {

      ExpandedQuery q = makeQuery("j");
      assertThat(rewriter.rewrite(q).getUserQuery(),
            bq(
            dmq(
                  term("j"),
                  bq(
                        dmq(must(), term("s")),
                        dmq(must(), term("t"))
                  ),
                  term("q")
            )
            ));

   }

   @Test
   public void testTwoClausesToOne() throws Exception {
      ExpandedQuery q = makeQuery("b c");
      assertThat(rewriter.rewrite(q).getUserQuery(),
            bq(
                  dmq(
                        term("b"),
                        bq(
                              dmq(must(), term("y")),
                              bq(
                                    mustNot(),
                                    dmq(must(), term("b")),
                                    dmq(must(), term("c")))
                        )
                  ),
                  dmq(
                        term("c"),
                        bq(

                              dmq(must(), term("y")),
                              bq(
                                    mustNot(),
                                    dmq(must(), term("b")),
                                    dmq(must(), term("c")))

                        )
                  )

            ));

   }

   @Test
   public void testThreeClausesToTwo() throws Exception {
      ExpandedQuery q = makeQuery("bb cc dd");
      assertThat(rewriter.rewrite(q).getUserQuery(),
            bq(
                  dmq(
                        term("bb"),
                        bq(
                              bq(
                                    must(),
                                    dmq(must(), term("z")),
                                    dmq(must(), term("x"))),
                              bq(
                                    mustNot(),
                                    dmq(must(), term("bb")),
                                    dmq(must(), term("cc")),
                                    dmq(must(), term("dd"))
                              )
                        )
                  ),
                  dmq(
                        term("cc"),
                        bq(

                              bq(
                                    must(),
                                    dmq(must(), term("z")),
                                    dmq(must(), term("x"))),
                              bq(
                                    mustNot(),
                                    dmq(must(), term("bb")),
                                    dmq(must(), term("cc")),
                                    dmq(must(), term("dd"))

                              )
                        )),
                  dmq(
                        term("dd"),
                        bq(

                              bq(
                                    must(),
                                    dmq(must(), term("z")),
                                    dmq(must(), term("x"))),
                              bq(
                                    mustNot(),
                                    dmq(must(), term("bb")),
                                    dmq(must(), term("cc")),
                                    dmq(must(), term("dd"))

                              )
                        ))

            ));

   }

   /**
    * Rules: b c => y b c d => z x
    * 
    * Input: b c d
    * 
    * Expected Output:
    * 
    * (b (y AND -(b AND c)) ((z AND x) AND -(b AND c AND d)) )
    * 
    * (c (y AND -(b AND c)) ((z AND x) AND -(b AND c AND d)) )
    * 
    * (d ((z AND x) AND -(b AND c AND d)) )
    * 
    * @throws Exception
    * 
    */
   @Test
   public void test08() throws Exception {
      ExpandedQuery q = makeQuery("b c d");
      assertThat(rewriter.rewrite(q).getUserQuery(),
            bq(
                  dmq(
                        term("b"),
                        bq(
                              dmq(must(), term("y")),
                              bq(
                                    mustNot(),
                                    dmq(must(), term("b")),
                                    dmq(must(), term("c"))
                              )
                        ),
                        bq(
                              bq(
                                    must(),
                                    dmq(must(), term("z")),
                                    dmq(must(), term("x"))),
                              bq(
                                    mustNot(),
                                    dmq(must(), term("b")),
                                    dmq(must(), term("c")),
                                    dmq(must(), term("d")))
                        )
                  ),
                  dmq(
                        term("c"),
                        bq(
                              dmq(must(), term("y")),
                              bq(
                                    mustNot(),
                                    dmq(must(), term("b")),
                                    dmq(must(), term("c"))
                              )
                        ),
                        bq(
                              bq(
                                    must(),
                                    dmq(must(), term("z")),
                                    dmq(must(), term("x"))),
                              bq(
                                    mustNot(),
                                    dmq(must(), term("b")),
                                    dmq(must(), term("c")),
                                    dmq(must(), term("d")))
                        )
                  ),
                  dmq(
                        term("d"),
                        bq(

                              bq(
                                    must(),
                                    dmq(must(), term("z")),
                                    dmq(must(), term("x"))),
                              bq(
                                    mustNot(),
                                    dmq(must(), term("b")),
                                    dmq(must(), term("c")),
                                    dmq(must(), term("d"))

                              )
                        ))

            ));
   }

}
