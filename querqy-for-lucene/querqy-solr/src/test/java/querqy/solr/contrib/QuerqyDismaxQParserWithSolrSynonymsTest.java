package querqy.solr.contrib;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;

import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.RewriteChain;
import querqy.solr.QuerqyDismaxQParser;

@SolrTestCaseJ4.SuppressSSL
public class QuerqyDismaxQParserWithSolrSynonymsTest extends SolrTestCaseJ4 {


   public static void index() throws Exception {

      assertU(adoc("id", "1", "f1", "a"));
      assertU(adoc("id", "2", "f1", "a"));
      assertU(adoc("id", "3", "f2", "a"));
      assertU(adoc("id", "4", "f1", "b"));

      assertU(commit());
   }

   @BeforeClass
   public static void beforeTests() throws Exception {
      initCore("contrib/solrconfig-QuerqyDismaxQParserWithSolrSynonymsTest.xml", "schema.xml");
      index();
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
            "//str[@name='parsedquery'][contains(.,'(f1:\"a b\"~2 f1:\"b c\"~2 f1:\"c d\"~2)^2.1')]",
            "//str[@name='parsedquery'][contains(.,'f2:\"a b c\"~3 f2:\"b c d\"~3)^3.9')]");

      req.close();

   }

   @Test
   public void testThatGeneratedTermsArePenalised() throws Exception {
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
   public void testThatGeneratedQueryFieldBoostsAreApplied() throws Exception {
      SolrQueryRequest req = req("q", "a",
            DisMaxParams.QF, "f1^2 f2^3",
            QuerqyDismaxQParser.GFB, "0.8",
            QuerqyDismaxQParser.GQF, "f2^10",
            "defType", "querqy",
            "debugQuery", "true");

      assertQ("Generated query field boosts not working",
            req,
            "//str[@name='parsedquery'][contains(.,'f1:a^2.0 | f2:a^3.0 | f2:x^10.0')]"
      );
      req.close();
   }
   
   @Test
   public void testThatGeneratedQueryFieldsAreApplied() throws Exception {
      SolrQueryRequest req = req("q", "a",
            DisMaxParams.QF, "f1^2 f2^3",
            QuerqyDismaxQParser.GFB, "0.8",
            QuerqyDismaxQParser.GQF, "f2 f4",
            "defType", "querqy",
            "debugQuery", "true");

      assertQ("Generated query fields not working",
            req,
            "//str[@name='parsedquery'][contains(.,'f1:a^2.0 | f2:a^3.0 | f2:x^2.4 | f4:x^0.8')]"
      );
      req.close();
   }
   

   public void verifyQueryString(SolrQueryRequest req, String q, String... expectedSubstrings) throws Exception {

      QuerqyDismaxQParser parser = new QuerqyDismaxQParser(q, null, req.getParams(), req, new RewriteChain(),
           new WhiteSpaceQuerqyParser(), null);
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
