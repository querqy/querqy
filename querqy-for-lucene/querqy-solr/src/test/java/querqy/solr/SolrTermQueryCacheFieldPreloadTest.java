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
public class SolrTermQueryCacheFieldPreloadTest extends SolrTestCaseJ4 {

    public void index() {
        assertU(adoc("id", "1", "f1", "a"));
        assertU(adoc("id", "2", "f1", "c", "f3", "x"));
        assertU(adoc("id", "3", "f4", "y"));
        assertU(adoc("id", "4", "f4", "y"));
        assertU(adoc("id", "5", "f4", "z"));
        assertU(commit());
    }

    @BeforeClass
    public static void beforeTest() throws Exception{
        initCore("solrconfig-cache-field-preload.xml", "schema.xml");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearIndex();
        index();
    }

    // Issue #18
    @Test
    public void testSearchingForNonExistentTermThatWasPreloadedForDifferentField() throws Exception {

        String q = "a";
        SolrQueryRequest req = req(
                 
                 CommonParams.Q, q,
                 DisMaxParams.QF, "f1 f3",
                 DisMaxParams.MM, "100%",
                 QueryParsing.OP, "AND",
                 "defType", "querqy",
                 "debugQuery", "true"
                 );
         

        assertQ("Partial preload went wrong",
                 req,
                 "//result[@name='response'][@numFound='1']"
                );

        req.close();

    }

    @Test
    public void testThatDocumentFrequencyCorrectionIsAppliedToPreloadedFields() throws Exception {

        String q = "y";
        SolrQueryRequest req = req(

                CommonParams.Q, q,
                DisMaxParams.QF, "f4",
                DisMaxParams.MM, "100%",
                QueryParsing.OP, "AND",
                "defType", "querqy",
                "debugQuery", "true"
        );


        assertQ("DFC failed for preloaded terms",
                req,
                "//result[@name='response'][@numFound='3']",
                "//str[@name='5'][not(contains(.,'1 = n, number of documents containing term'))]",
                "//str[@name='5'][contains(.,'2 = n, number of documents containing term')]",
                "//str[@name='4'][not(contains(.,'1 = n, number of documents containing term'))]",
                "//str[@name='4'][contains(.,'2 = n, number of documents containing term')]",
                "//str[@name='3'][not(contains(.,'1 = n, number of documents containing term'))]",
                "//str[@name='4'][contains(.,'2 = n, number of documents containing term')]"
        );

        req.close();

    }
}
