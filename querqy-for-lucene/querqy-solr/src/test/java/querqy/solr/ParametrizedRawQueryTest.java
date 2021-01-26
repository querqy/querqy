package querqy.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class ParametrizedRawQueryTest extends SolrTestCaseJ4 {

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-commonrules-parametrized-raw-query.xml", "schema.xml");
        addDocs();
    }

    private static void addDocs() {
        assertU(adoc("id", "1", "f1", "term1", "str", "boostval1"));
        assertU(adoc("id", "2", "f1", "term1", "str", "boostval2"));
        assertU(adoc("id", "3", "f1", "term3", "str", "link's awakening"));
        assertU(adoc("id", "id-4", "f1", "term4", "str", "filterval1"));
        assertU(adoc("id", "id-5", "f1", "term4", "str", "filterval2"));
        assertU(adoc("id", "id-6", "f1", "term4", "str", "filterval3"));
        assertU(commit());
    }

    @Test
    public void testMultipleParamsFromDifferentRewriters() {
        String q = "term4";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "uq.similarityScore", "off",
                "fl", "id,score",
                "debugQuery", "on",
                "defType", "querqy");

        assertQ("",
                req,
                "//result[@name='response' and @numFound='1']"
        );
        req.close();
    }

    @Test
    public void testSingleParam() {
        String q = "term1";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "uq.similarityScore", "off",
                "fl", "id,score",
                "debugQuery", "on",
                "defType", "querqy");

        assertQ("",
                req,
                "//result[@name='response' and @numFound='2']",
                "//result[@name='response']/doc[1]/str[@name='id'][text()='1']",
                "//result[@name='response']/doc[1]/float[@name='score'][text()='101.0']",
                "//result[@name='response']/doc[2]/str[@name='id'][text()='2']",
                "//result[@name='response']/doc[2]/float[@name='score'][text()='1.0']"
        );
        req.close();
    }

    @Test
    public void testMultipleParams() {
        String q = "term2";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "uq.similarityScore", "off",
                "fl", "id,score",
                "debugQuery", "on",
                "defType", "querqy");

        assertQ("",
                req,
                "//result[@name='response' and @numFound='2']",
                "//result[@name='response']/doc[1]/str[@name='id'][text()='2']",
                "//result[@name='response']/doc[1]/float[@name='score'][text()='51.0']",
                "//result[@name='response']/doc[2]/str[@name='id'][text()='1']",
                "//result[@name='response']/doc[2]/float[@name='score'][text()='11.0']"
        );
        req.close();
    }

    @Test
    public void testPhrase() {
        String q = "term3";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "uq.similarityScore", "off",
                "fl", "id,score",
                "debugQuery", "on",
                "defType", "querqy");

        assertQ("",
                req,
                "//result[@name='response' and @numFound='1']",
                "//result[@name='response']/doc[1]/str[@name='id'][text()='3']",
                "//result[@name='response']/doc[1]/float[@name='score'][text()='101.0']"
        );
        req.close();
    }
}
