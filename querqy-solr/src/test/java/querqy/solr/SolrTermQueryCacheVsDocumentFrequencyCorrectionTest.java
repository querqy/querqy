package querqy.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.junit.BeforeClass;
import org.junit.Test;

public class SolrTermQueryCacheVsDocumentFrequencyCorrectionTest extends SolrTestCaseJ4 {
    
    public static void index() throws Exception {

        assertU(adoc("id", "1", "f1", "a"));

        assertU(adoc("id", "2", "f1", "b", "f2", "c"));

        assertU(commit());
     }

    @BeforeClass
    public static void beforeClass() throws Exception {
        System.setProperty("tests.codec", "Lucene50");
        initCore("solrconfig-cache-vs-documentfrequencycorrection.xml", "schema.xml");
        index();
    }
         

    @Test
    public void testCacheVsDocumentFrequencyCorrection() {
        // The rules contain an UP rule that never matches. Make
        // sure this doesn't cause an Exception in the DocumentFrequencyCorrection
        String q = "a";

        SolrQueryRequest req = req("q", q,
              DisMaxParams.QF, "f1 f2",
              QueryParsing.OP, "OR",
              DisMaxParams.TIE, "0.1",
              "defType", "querqy",
              "debugQuery", "true"
              );
        assertQ("Two results expected",
                req,
                "//result[@name='response'][@numFound='2']");

        req.close();
    }

}
