package querqy.solr;

import static org.junit.Assert.*;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.junit.BeforeClass;
import org.junit.Test;

public class ShingleRewriterTest extends SolrTestCaseJ4 {
    
   

     @BeforeClass
     public static void beforeClass() throws Exception {
        System.setProperty("tests.codec", "Lucene46");
        initCore("solrconfig-shingles-and-commonrules.xml", "schema.xml");
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

}
