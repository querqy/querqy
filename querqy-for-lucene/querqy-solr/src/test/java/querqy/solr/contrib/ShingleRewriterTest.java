package querqy.solr.contrib;


import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class ShingleRewriterTest extends SolrTestCaseJ4 {

     @BeforeClass
     public static void beforeTests() throws Exception {
        initCore("contrib/solrconfig-shingles-and-commonrules.xml", "schema.xml");
     }

    @Test
    public void testShinglesOnThreeTerms() {
        String q = "a b c";

        SolrQueryRequest req = req("q", q,
              DisMaxParams.QF, "f1 f2 f3",
              "defType", "querqy",
              "debugQuery", "on"
              );

        assertQ("Missing shingles",
              req,
              "//str[@name='parsedquery'][contains(.,'ab')]",
              "//str[@name='parsedquery'][contains(.,'bc')]"

        );

        req.close();    
    }
    
    @Test
    public void testShinglesAsInputOnCommonRules() {
        String q = "a b c";

        SolrQueryRequest req = req("q", q,
              DisMaxParams.QF, "f1 f2 f3",
              "defType", "querqy",
              "debugQuery", "on"
              );

        assertQ("Missing shingles",
              req,
              "//str[@name='parsedquery'][contains(.,'shingleab')]",
              "//str[@name='parsedquery'][contains(.,'synonymforc')]"

        );

        req.close();    
    }
    
    @Test
    public void testThatShinglesAreNotCreatedOnGeneratedTerms() throws Exception {
        String q = "t1 t2";

        SolrQueryRequest req = req("q", q,
              DisMaxParams.QF, "f1",
              "defType", "querqy",
              "debugQuery", "on"
              );

        assertQ("Problem with shingles on generated terms",
              req,
              "//str[@name='parsedquery'][contains(.,'t1t2')]",
              "//str[@name='parsedquery'][not(contains(.,'s1t2'))]"

        );

        req.close();    
    }

}
