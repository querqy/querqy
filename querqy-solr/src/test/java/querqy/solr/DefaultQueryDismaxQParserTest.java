package querqy.solr;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.junit.BeforeClass;
import org.junit.Test;

import querqy.lucene.rewrite.IndexStats;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.RewriteChain;

public class DefaultQueryDismaxQParserTest extends SolrTestCaseJ4 {

   static final IndexStats DUMMY_INDEX_STATS = new IndexStats() {

      @Override
      public int df(Term term) {
         return 10;
      }
   };

   public static void index() throws Exception {

      assertU(adoc("id", "1", "f1", "a"));
      assertU(adoc("id", "2", "f1", "a"));
      assertU(adoc("id", "3", "f2", "a"));
      assertU(adoc("id", "4", "f1", "b"));

      assertU(commit());
   }

   @BeforeClass
   public static void beforeClass() throws Exception {
      System.setProperty("tests.codec", "Lucene46");
      initCore("solrconfig.xml", "schema.xml");
      index();
   }

   @Test
   public void testThatAMMof2getsSetFor3optionalClauses() throws Exception {

      SolrQueryRequest req = req("q", "a b c",
            DisMaxParams.QF, "f1 f2",
            DisMaxParams.MM, "2");

      QuerqyDismaxQParser parser = new QuerqyDismaxQParser("a b c", null, req.getParams(), req, new RewriteChain(),
            DUMMY_INDEX_STATS, new WhiteSpaceQuerqyParser());

      Query query = parser.parse();

      req.close();

      assertTrue(query instanceof BooleanQuery);
      BooleanQuery bq = (BooleanQuery) query;
      assertEquals(2, bq.getMinimumNumberShouldMatch());
   }

   @Test
   public void testThatMMIsAppliedWhileQueryContainsMUSTBooleanOperators() throws Exception {

      String q = "a +b c +d e f";

      SolrQueryRequest req = req("q", q,
            DisMaxParams.QF, "f1 f2",
            DisMaxParams.MM, "3");
      QuerqyDismaxQParser parser = new QuerqyDismaxQParser(q, null, req.getParams(), req, new RewriteChain(),
            DUMMY_INDEX_STATS, new WhiteSpaceQuerqyParser());
      Query query = parser.parse();

      req.close();

      assertTrue(query instanceof BooleanQuery);
      BooleanQuery bq = (BooleanQuery) query;
      assertEquals(3, bq.getMinimumNumberShouldMatch());
   }

   @Test
   public void testThatMMIsNotAppliedWhileQueryContainsMUSTNOTBooleanOperator() throws Exception {

      String q = "a +b c -d e f";

      SolrQueryRequest req = req("q", q,
            DisMaxParams.QF, "f1 f2",
            DisMaxParams.MM, "3",
            QueryParsing.OP, "OR"
            );
      QuerqyDismaxQParser parser = new QuerqyDismaxQParser(q, null, req.getParams(), req, new RewriteChain(),
            DUMMY_INDEX_STATS, new WhiteSpaceQuerqyParser());
      Query query = parser.parse();

      req.close();

      assertTrue(query instanceof BooleanQuery);
      BooleanQuery bq = (BooleanQuery) query;
      assertEquals(0, bq.getMinimumNumberShouldMatch());
   }

   @Test
   public void testThatPfIsAppliedOnlyToExistingField() throws Exception {

      String q = "a b c d";

      SolrQueryRequest req = req("q", q,
            DisMaxParams.QF, "f1 f2",
            DisMaxParams.MM, "3",
            QueryParsing.OP, "OR",
            DisMaxParams.PF, "f3^2 f40^0.5",
            "defType", "querqy",
            "debugQuery", "true"
            );

      // f4 doesn't exist
      assertQ("wrong ps",
            req,
            "//str[@name='parsedquery'][contains(.,'f3:\"a b c d\"^2.0')]",
            "//str[@name='parsedquery'][not(contains(.,'f40:\"a b c d\"^0.5'))]");

      req.close();

   }

   @Test
   public void testThatMatchAllDoesNotThrowException() throws Exception {
      String q = "*:*";

      SolrQueryRequest req = req("q", q,
            DisMaxParams.QF, "f1 f2",
            DisMaxParams.MM, "3",
            QueryParsing.OP, "OR",
            DisMaxParams.PF, "f3^2 f4^0.5",
            "defType", "querqy",
            "debugQuery", "true"
            );

      assertQ("Matchall fails",
            req,
            "//str[@name='parsedquery'][contains(.,'*:*')]",
            "//result[@name='response' and @numFound='4']"

      );

      req.close();
   }

   @Test
   public void testThatPfIsAppliedOnlyToFieldsWithTermPositions() throws Exception {

      String q = "a b c d";
      SolrQueryRequest req = req("q", q,
            DisMaxParams.QF, "f1 f2",
            DisMaxParams.MM, "3",
            QueryParsing.OP, "OR",
            "defType", "querqy",
            "debugQuery", "true",
            DisMaxParams.PF, "f1^1.2 str^2 f_no_tfp^0.5");

      // str is a string field
      // f_no_tfp / f_no_tp don't have term positions
      assertQ("wrong ps2",
            req,
            "//str[@name='parsedquery'][contains(.,'f1:\"a b c d\"^1.2')]",
            "//str[@name='parsedquery'][not(contains(.,'str:\"a b c d\"^2.0'))]",
            "//str[@name='parsedquery'][not(contains(.,'f_no_tfp:\"a b c d\"^0.5'))]",
            "//str[@name='parsedquery'][not(contains(.,'f_no_tp:\"a b c d\"^0.5'))]");

      req.close();

   }

   @Test
   public void testThatPf2IsAppliedOnlyToExistingField() throws Exception {

      String q = "a b c d";
      SolrQueryRequest req = req("q", q,
            DisMaxParams.QF, "f1 f2",
            DisMaxParams.MM, "3",
            QueryParsing.OP, "OR",
            "defType", "querqy",
            "debugQuery", "true",
            DisMaxParams.PF2, "f1^2 f40^0.5");

      // f40 does not exists
      assertQ("wrong ps2",
            req,
            "//str[@name='parsedquery'][contains(.,'f1:\"a b\"^2.0')]",
            "//str[@name='parsedquery'][contains(.,'f1:\"b c\"^2.0')]",
            "//str[@name='parsedquery'][contains(.,'f1:\"c d\"^2.0')]",
            "//str[@name='parsedquery'][not(contains(.,'f40:\"a b\"^0.5'))]",
            "//str[@name='parsedquery'][not(contains(.,'f40:\"b c\"^0.5'))]",
            "//str[@name='parsedquery'][not(contains(.,'f40:\"c d\"^0.5'))]");

      req.close();

   }

   @Test
   public void testThatPf2IsAppliedOnlyToFieldsWithTermPositions() throws Exception {

      String q = "a b c d";
      SolrQueryRequest req = req("q", q,
            DisMaxParams.QF, "f1 f2",
            DisMaxParams.MM, "3",
            QueryParsing.OP, "OR",
            "defType", "querqy",
            "debugQuery", "true",
            DisMaxParams.PF2, "f1^1.2 str^2 f_no_tfp^0.5");

      // str is a string field
      // f_no_tfp doesn't have term positions
      assertQ("wrong ps2",
            req,
            "//str[@name='parsedquery'][contains(.,'f1:\"a b\"^1.2')]",
            "//str[@name='parsedquery'][contains(.,'f1:\"b c\"^1.2')]",
            "//str[@name='parsedquery'][contains(.,'f1:\"c d\"^1.2')]",
            "//str[@name='parsedquery'][not(contains(.,'str:\"a b\"^2.0'))]",
            "//str[@name='parsedquery'][not(contains(.,'str:\"b c\"^2.0'))]",
            "//str[@name='parsedquery'][not(contains(.,'str:\"c d\"^2.0'))]",
            "//str[@name='parsedquery'][not(contains(.,'f_no_tfp:\"a b\"^0.5'))]",
            "//str[@name='parsedquery'][not(contains(.,'f_no_tfp:\"b c\"^0.5'))]",
            "//str[@name='parsedquery'][not(contains(.,'f_no_tfp:\"c d\"^0.5'))]");

      req.close();

   }

   @Test
   public void testThatPf3IsAppliedOnlyToFieldsWithTermPositions() throws Exception {

      String q = "a b c d";
      SolrQueryRequest req = req("q", q,
            DisMaxParams.QF, "f1 f2",
            DisMaxParams.MM, "3",
            QueryParsing.OP, "OR",
            "defType", "querqy",
            "debugQuery", "true",
            DisMaxParams.PF3, "f1^1.2 str^2 f_no_tfp^0.5 f4^4");

      // str is a string field
      // f_no_tfp / f_no_tp don't have term positions
      assertQ("wrong ps2",
            req,
            "//str[@name='parsedquery'][contains(.,'f1:\"a b c\"^1.2')]",
            "//str[@name='parsedquery'][contains(.,'f1:\"b c d\"^1.2')]",
            "//str[@name='parsedquery'][not(contains(.,'str:\"a b c\"^2.0'))]",
            "//str[@name='parsedquery'][not(contains(.,'str:\"b c d\"^2.0'))]",
            "//str[@name='parsedquery'][not(contains(.,'f_no_tfp:\"a b c\"^0.5'))]",
            "//str[@name='parsedquery'][not(contains(.,'f_no_tfp:\"b c d\"^0.5'))]",
            "//str[@name='parsedquery'][not(contains(.,'f_no_tp:\"a b c\"^0.5'))]",
            "//str[@name='parsedquery'][not(contains(.,'f_no_tp:\"b c d\"^0.5'))]");

      req.close();

   }

   @Test
   public void testThatPf3IsApplied() throws Exception {

      String q = "a b c d";
      SolrQueryRequest req = req("q", q,
            DisMaxParams.QF, "f1 f2",
            DisMaxParams.MM, "3",
            QueryParsing.OP, "OR",
            DisMaxParams.PF3, "f2^2.5 f3^1.5"
            );
      verifyQueryString(req, q,
            "f2:\"a b c\"^2.5", "f2:\"b c d\"^2.5",
            "f3:\"a b c\"^1.5", "f3:\"b c d\"^1.5"

      );

   }

   @Test
   public void testThatPFSkipsMustNotClauses() throws Exception {

      String q = "a b -c d e f";

      SolrQueryRequest req = req("q", q,
            DisMaxParams.QF, "f1 f2",
            DisMaxParams.MM, "3",
            QueryParsing.OP, "OR",
            DisMaxParams.PF, "f2^1.5 f3^1.5",
            DisMaxParams.PF2, "f1^2.1 f2^2.1",
            DisMaxParams.PF3, "f3^3.9 f1^3.9"
            );

      verifyQueryString(req, q,
            "f2:\"a b d e f\"^1.5", "f3:\"a b d e f\"^1.5",
            "f1:\"a b\"^2.1", "f1:\"b d\"^2.1", "f1:\"d e\"^2.1", "f1:\"e f\"^2.1",
            "f2:\"a b\"^2.1", "f2:\"b d\"^2.1", "f2:\"d e\"^2.1", "f2:\"e f\"^2.1",
            "f3:\"a b d\"^3.9", "f3:\"b d e\"^3.9", "f3:\"d e f\"^3.9",
            "f1:\"a b d\"^3.9", "f1:\"b d e\"^3.9", "f1:\"d e f\"^3.9"

      );

   }

   @Test
   public void testThatPFWorksWithSynonymRewriting() throws Exception {

      SolrQueryRequest req = req("q", "a b",
            DisMaxParams.QF, "f1 f2^0.9",
            DisMaxParams.PF, "f1^0.5",
            "defType", "querqy",
            "debugQuery", "true");

      assertQ("ps with synonyms not working",
            req,
            "//str[@name='parsedquery'][contains(.,'f1:\"a b\"^0.5')]");

      req.close();

   }

   @Test
   public void testThatPF23FWorksWithSynonymRewriting() throws Exception {

      SolrQueryRequest req = req("q", "a b c d",
            DisMaxParams.QF, "f1 f2^0.9",
            DisMaxParams.PF2, "f1~2^2.1",
            DisMaxParams.PF3, "f2~3^3.9",
            "defType", "querqy",
            "debugQuery", "true");

      assertQ("ps2/3 with synonyms not working",
            req,
            "//str[@name='parsedquery'][contains(.,'f1:\"a b\"~2^2.1')]",
            "//str[@name='parsedquery'][contains(.,'f1:\"b c\"~2^2.1')]",
            "//str[@name='parsedquery'][contains(.,'f1:\"c d\"~2^2.1')]",
            "//str[@name='parsedquery'][contains(.,'f2:\"a b c\"~3^3.9')]",
            "//str[@name='parsedquery'][contains(.,'f2:\"b c d\"~3^3.9')]");

      req.close();

   }

   @Test
   public void testThatGeneratedTermsArePanalised() throws Exception {
      SolrQueryRequest req = req("q", "a b",
            DisMaxParams.QF, "f1^2",
            DisMaxParams.PF, "f1^0.5",
            QuerqyDismaxQParser.GFB, "0.8",
            "defType", "querqy",
            "debugQuery", "true");

      assertQ(QuerqyDismaxQParser.GFB + " not working",
            req,
            "//str[@name='parsedquery'][contains(.,'f1:a^2.0 | f1:x^1.6')]",
            "//str[@name='parsedquery'][contains(.,'f1:\"a b\"^0.5')]");

      req.close();
   }
   
   @Test
   public void testThatGeneratedQueryFieldsAreApplied() throws Exception {
      SolrQueryRequest req = req("q", "a",
            DisMaxParams.QF, "f1^2 f2^3",
            QuerqyDismaxQParser.GFB, "0.8",
            QuerqyDismaxQParser.GQF, "f2^10 f4",
            "defType", "querqy",
            "debugQuery", "true");

      assertQ("Generated query fields not working",
            req,
            "//str[@name='parsedquery'][contains(.,'f1:a^2.0 | f2:a^3.0 | f2:x^10.0 | f4:x^0.8')]"
      );
      req.close();
   }
   

   public void verifyQueryString(SolrQueryRequest req, String q, String... expectedSubstrings) throws Exception {

      QuerqyDismaxQParser parser = new QuerqyDismaxQParser(q, null, req.getParams(), req, new RewriteChain(),
            DUMMY_INDEX_STATS, new WhiteSpaceQuerqyParser());
      Query query = parser.parse();
      req.close();
      assertTrue(query instanceof BooleanQuery);
      BooleanQuery bq = (BooleanQuery) query;
      String qStr = bq.toString();
      for (String exp : expectedSubstrings) {
         assertTrue("Missing: " + exp, qStr.indexOf(exp) > -1);
      }

   }

}
