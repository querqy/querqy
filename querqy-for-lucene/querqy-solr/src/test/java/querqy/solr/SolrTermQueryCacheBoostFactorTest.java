package querqy.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class SolrTermQueryCacheBoostFactorTest extends SolrTestCaseJ4 {

    public void index() throws Exception {

        assertU(adoc("id", "1", "f1", "a"));
        assertU(adoc("id", "2", "f1", "a", "f2", "b"));
        assertU(adoc("id", "3", "f1", "a", "f2", "c"));
        assertU(commit());
    }

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-cache.xml", "schema.xml");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearIndex();
        index();
    }
    
    @Test
    public void testThatRequestDependentBoostFactorsAreApplied() throws Exception {
        String q = "a c";

        SolrQueryRequest req = req("q", q,
              DisMaxParams.QF, "f1^10 f2^200",
              QueryParsing.OP, "OR",
              QuerqyDismaxQParser.GFB, "0.4",
              DisMaxParams.TIE, "0.1",
              "defType", "querqy",
              "debugQuery", "true"
        );        
        
        assertQ("Boost factors are not applied to terms", 
                req, 
                "//str[@name='parsedquery'][contains(.,'f1:a^10.0 | f2:a^200.0 | f1:b^4.0 | f2:b^80.0')]",
                "//str[@name='parsedquery'][contains(.,'f1:c^10.0 | f2:c^200.0')]");
        req.close();
        
        SolrQueryRequest  req2 = req("q", q,
                DisMaxParams.QF, "f1^88 f2^1600",
                QueryParsing.OP, "OR",
                QuerqyDismaxQParser.GFB, "0.25",
                DisMaxParams.TIE, "0.1",
                "defType", "querqy",
                "debugQuery", "true"
        );        
          
        assertQ("Boost factors are not applied to terms", 
                req2, 
                "//str[@name='parsedquery'][contains(.,'f1:a^88.0 | f2:a^1600.0 | f1:b^22.0 | f2:b^400.0')]",
                "//str[@name='parsedquery'][contains(.,'f1:c^88.0 | f2:c^1600.0')]");
        
        // make sure we've hit the cache
        SolrQueryRequest reqStats = req(
                CommonParams.QT, "/admin/mbeans",
                "cat", "CACHE",
                "stats", "true"
                );
        // should be 6 hits (2 fields x 3 keywords)
        assertQ("Querqy cache not hit",
                 reqStats,
               "//lst[@name='CACHE']/lst[@name='querqyTermQueryCache']"
                       + "/lst[@name='stats']/long[@name='CACHE.searcher.querqyTermQueryCache.hits'][text()='6']");
        
         
    }
}
