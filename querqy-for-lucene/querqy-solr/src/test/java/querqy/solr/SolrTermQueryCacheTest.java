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
public class SolrTermQueryCacheTest extends SolrTestCaseJ4 {

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
    public void testThatCacheIsAvailable() throws Exception {
         
         SolrQueryRequest req = req(
               CommonParams.QT, "/admin/mbeans",
               "cat", "CACHE",
               "stats", "true"
               );
         assertQ("Missing querqy cache",
               req,
               "//lst[@name='CACHE']/lst[@name='querqyTermQueryCache']");

         req.close();
    }
    
    @Test
    public void testThatTermQueriesArePutIntoAndServedFromCache() throws Exception {
        
        String q = "c";

        SolrQueryRequest req = req("q", q,
              DisMaxParams.QF, "f1 f2",
              QueryParsing.OP, "OR",
              DisMaxParams.TIE, "0.1",
              "defType", "querqy",
              "debugQuery", "true"
        );        
        
        assertQ("Unexpected query result while caching", 
                req, 
                "//result[@name='response'][@numFound='1']");
        req.close();
        
         
        SolrQueryRequest reqStats = req(
               CommonParams.QT, "/admin/mbeans",
               "cat", "CACHE",
               "stats", "true"
               );
        assertQ("Missing querqy cache",
                reqStats,
              "//lst[@name='CACHE']/lst[@name='querqyTermQueryCache']"
                      + "/lst[@name='stats']/long[@name='CACHE.searcher.querqyTermQueryCache.lookups'][text()='2']");
        assertQ("Missing querqy cache",
                reqStats,
              "//lst[@name='CACHE']/lst[@name='querqyTermQueryCache']"
                      + "/lst[@name='stats']/long[@name='CACHE.searcher.querqyTermQueryCache.hits'][text()='0']");
        assertQ("Missing querqy cache",
                reqStats,
              "//lst[@name='CACHE']/lst[@name='querqyTermQueryCache']"
                      + "/lst[@name='stats']/long[@name='CACHE.searcher.querqyTermQueryCache.size'][text()='2']");

        reqStats.close();
        
        SolrQueryRequest req2 = req("q", q,
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR",
                DisMaxParams.TIE, "0.1",
                "defType", "querqy",
                "debugQuery", "true"
          );        
          
          assertQ("Unexpected query result while using cache", 
                  req2, 
                  "//result[@name='response'][@numFound='1']");
          req2.close();
          
           
          SolrQueryRequest reqStats2 = req(
                 CommonParams.QT, "/admin/mbeans",
                 "cat", "CACHE",
                 "stats", "true"
                 );
          assertQ("Missing querqy cache",
                  reqStats2,
                "//lst[@name='CACHE']/lst[@name='querqyTermQueryCache']"
                        + "/lst[@name='stats']/long[@name='CACHE.searcher.querqyTermQueryCache.lookups'][text()='4']");
          assertQ("Missing querqy cache",
                  reqStats2,
                "//lst[@name='CACHE']/lst[@name='querqyTermQueryCache']"
                        + "/lst[@name='stats']/long[@name='CACHE.searcher.querqyTermQueryCache.hits'][text()='2']");
          assertQ("Missing querqy cache",
                  reqStats2,
                "//lst[@name='CACHE']/lst[@name='querqyTermQueryCache']"
                        + "/lst[@name='stats']/long[@name='CACHE.searcher.querqyTermQueryCache.size'][text()='2']");

          reqStats2.close();
    }
}
