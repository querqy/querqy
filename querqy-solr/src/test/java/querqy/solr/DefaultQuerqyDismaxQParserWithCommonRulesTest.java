package querqy.solr;

import static org.junit.Assert.*;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.junit.BeforeClass;
import org.junit.Test;

public class DefaultQuerqyDismaxQParserWithCommonRulesTest extends SolrTestCaseJ4 {
    
    public static void index() throws Exception {

        assertU(adoc("id", "1", "f1", "a", "f2", "c"));

        assertU(adoc("id", "2", "f1", "a", "f2", "b"));
        
        assertU(adoc("id", "3", "f1", "a", "f3", "c"));
        


        assertU(commit());
     }

     @BeforeClass
     public static void beforeClass() throws Exception {
        System.setProperty("tests.codec", "Lucene46");
        initCore("solrconfig-commonrules.xml", "schema.xml");
        index();
     }

     
     

    @Test
    public void testSolrFilterQuery() {
        
        String q = "a k";

        SolrQueryRequest req = req("q", q,
              DisMaxParams.QF, "f1 f2 f3",
              DisMaxParams.MM, "1",
              QueryParsing.OP, "OR",
              "defType", "querqy"
              );

        assertQ("Solr filter query fails",
              req,
              "//result[@name='response' and @numFound='1']"

        );

        req.close();
        
        
    }
    
    
    @Test
    public void testDeleteIsAppliedInContext() throws Exception {
        String q = "t1 t2";

        SolrQueryRequest req = req("q", q,
              DisMaxParams.QF, "f1 f2 f3",
              DisMaxParams.MM, "1",
              QueryParsing.OP, "OR",
              "defType", "querqy",
              "debugQuery", "on"
              );

        assertQ("Delete in context fails",
              req,
              "//str[@name='parsedquery'][not(contains(.,'t2'))]"
        );

        req.close();
    }
    
    @Test
    public void testThatRuleMatchingIsCaseInsensitive() throws Exception {
        String q = "T1 T2";

        SolrQueryRequest req = req("q", q,
              DisMaxParams.QF, "f1 f2 f3",
              DisMaxParams.MM, "1",
              QueryParsing.OP, "OR",
              "defType", "querqy",
              "debugQuery", "on"
              );

        assertQ("Rule matching seems to be case sensitive",
              req,
              "//str[@name='parsedquery'][not(contains(.,'t2'))]",
              "//str[@name='parsedquery'][not(contains(.,'T2'))]"
        );

        req.close();
    }

}
