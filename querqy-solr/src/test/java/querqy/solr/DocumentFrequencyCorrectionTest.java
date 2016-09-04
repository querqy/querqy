package querqy.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.junit.BeforeClass;
import org.junit.Test;

public class DocumentFrequencyCorrectionTest extends SolrTestCaseJ4 {

    public static void index() throws Exception {

      assertU(adoc("id", "1", "f1", "a"));

      assertU(commit());
      assertU(adoc("id", "2", "f1", "a", "f2", "b"));

      assertU(adoc("id", "3", "f1", "a", "f2", "c"));

      assertU(adoc("id", "4", "f1", "a", "f2", "k"));
      assertU(commit());
      assertU(adoc("id", "5", "f1", "a", "f2", "k"));
      assertU(adoc("id", "6", "f1", "a", "f2", "k"));
      assertU(adoc("id", "7", "f1", "a", "f2", "k"));

      assertU(commit());
   }

   @BeforeClass
   public static void beforeTests() throws Exception {
      initCore("solrconfig-boost.xml", "schema.xml");
      index();
   }

   @Test
   public void testDfGetsCorrectedForBoostUp() throws Exception {

      String q = "a c";

      SolrQueryRequest req = req("q", q,
            DisMaxParams.QF, "f1 f2",
            QueryParsing.OP, "OR",
            DisMaxParams.TIE, "0.1",
            "defType", "querqy",
            "debugQuery", "true"
            );
      assertQ("wrong df",
            req,
            "//str[@name='2'][contains(.,'docFreq=7')]",
            "//str[@name='2'][not(contains(.,'docFreq=1'))]",
            "//str[@name='7'][contains(.,'docFreq=10')]",
            "//str[@name='7'][not(contains(.,'docFreq=4'))]");

      req.close();
   }

}
