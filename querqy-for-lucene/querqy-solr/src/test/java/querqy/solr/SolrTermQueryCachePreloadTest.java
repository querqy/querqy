package querqy.solr;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.junit.BeforeClass;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class SolrTermQueryCachePreloadTest extends SolrTestCaseJ4 {

    @BeforeClass
    public static void beforeTest() throws Exception{
        initCore("solrconfig.xml", "schema.xml", getFile("cache-preload-test/collection1").getParent());
        withCommonRulesRewriter(h.getCore(), "common_rules", "configs/commonrules/rules-cache.txt");

        // this leaves the rewriter file in place so that it will be available
        // for the firstSearcher event in testThatCacheIsAvailableAndPrefilledNotUpdatedByQueryAndUpdatedByRewriter()
        h.close();
        initCore("solrconfig.xml", "schema.xml", getFile("cache-preload-test/collection1").getParent());
    }
     
    @Test
    public void testThatCacheIsAvailableAndPrefilledNotUpdatedByQueryAndUpdatedByRewriter() throws Exception {

        // firstSearcher
        SolrQueryRequest req = req(
               CommonParams.QT, "/admin/mbeans",
               "cat", "CACHE",
               "stats", "true"
               );
        // the cache is prefilled asynchronously - try 3 times to see the cache before giving up
        int attempts = 20;
        try {

            do {

                try {
                    assertQ("Missing querqy cache",
                       req,
                       "//lst[@name='CACHE']/lst[@name='querqyTermQueryCache'][text()='1']");
                    attempts = 0;
                }  catch (Exception e) {
                    if (attempts <= 1) {
                        throw e;
                    }
                    attempts--;
                    synchronized(this) {
                        wait(100L);
                    }
                }
            } while (attempts > 0);

             // only one generated term in one field is preloaded for firstSearcher:
            assertQ("Querqy cache not prefilled",
                     req,
                    "//lst[@name='CACHE']/lst[@name='querqyTermQueryCache']"
                            + "/lst[@name='stats']/long[@name='CACHE.searcher.querqyTermQueryCache.size'][text()='1']");
        } finally {
            req.close();
        }
        
        assertU(adoc("id", "1", "f1", "a"));
        assertU(commit());
         
        // newSearcher
        SolrQueryRequest req2 = req(
                 CommonParams.QT, "/admin/mbeans",
                 "cat", "CACHE",
                 "stats", "true"
                 );
         
        // one generated term in two fields is preloaded for the newSearcher event (which preloads for f1 and f2, while
        // firstSearch only preloads for a single field):
        assertQ("Querqy cache not prefilled",
                 req2,
                 "//lst[@name='CACHE']/lst[@name='querqyTermQueryCache']"
                         + "/lst[@name='stats']/long[@name='CACHE.searcher.querqyTermQueryCache.size'][text()='2']");

        req2.close();
         
        String q = "a b c";
        SolrQueryRequest req3 = req(
                 
                CommonParams.Q, q,
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "AND",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules"
                 );
         
        // f1:b and f2:b would be produced by synonym rule, but
        // due to pre-testing for hits in preload they should not
        // occur in the parsed query
        assertQ("Terms w/o hits found in parsedquery",
                 req3,
                 "//result[@name='response'][@numFound='0']",
                 "//str[@name='parsedquery'][not(contains(.,'f1:b'))]",
                 "//str[@name='parsedquery'][not(contains(.,'f2:b'))]"
                );

        req3.close();
         
         
        SolrQueryRequest reqStats = req(
                 CommonParams.QT, "/admin/mbeans",
                 "cat", "CACHE",
                 "stats", "true"
                 );
         
        assertQ("Querqy cache was updated unexpectedly",
                 reqStats,
                 "//lst[@name='CACHE']/lst[@name='querqyTermQueryCache']"
                         + "/lst[@name='stats']/long[@name='CACHE.searcher.querqyTermQueryCache.size'][text()='2']");

        reqStats.close();

        withCommonRulesRewriter(h.getCore(), "common_rules", "configs/commonrules/rules-cache-update.txt");

        SolrQueryRequest reqAfterReloaderUpdate = req(
                CommonParams.QT, "/admin/mbeans",
                "cat", "CACHE",
                "stats", "true"
        );

        // the cache is prefilled asynchronously - try 3 times to see the cache update before giving up
        attempts = 20;
        try {

            do {

                try {
                    // the new rules produce 2 rhs terms, which are searched in 2 fields each
                    assertQ("common_rules update didn't trigger preloader",
                            reqAfterReloaderUpdate,
                            "//lst[@name='CACHE']/lst[@name='querqyTermQueryCache']/lst[@name='stats']/" +
                                    "long[@name='CACHE.searcher.querqyTermQueryCache.size'][text()='4']");
                    attempts = 0;

                }  catch (Exception e) {
                    if (attempts <= 1) {
                        throw e;
                    }
                    attempts--;
                    synchronized(this) {
                        wait(100L);
                    }
                }
            } while (attempts > 0);


        } finally {
            reqAfterReloaderUpdate.close();
        }
         
         
    }
   
}
