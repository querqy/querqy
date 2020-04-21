package querqy.solr.contrib;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class NumberUnitRewriterTest extends SolrTestCaseJ4 {

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("contrib/solrconfig-numberunit.xml", "contrib/schema-numberunit.xml");
        addDocs();
    }

    private static void addDocs() {
        assertU(adoc("id", "1", "f1", "tv", "f2", "tele", "depth", "2", "width", "200", "screen_size", "55"));
        assertU(adoc("id", "2", "f1", "tv", "height", "130", "depth", "2", "width", "190", "screen_size", "54.6"));
        assertU(adoc("id", "3", "f1", "tv", "height", "110", "depth", "10", "width", "160", "screen_size", "50"));
        assertU(adoc("id", "4", "f1", "tv", "height", "80", "depth", "2", "width", "120", "screen_size", "35.7"));
        assertU(adoc("id", "5", "f1", "tv", "fieldUnlimited", "100000"));

        assertU(adoc("id", "6", "f1", "notebook", "disk", "1150", "screen_size", "14.8", "fieldUnlimited", "0"));
        assertU(adoc("id", "7", "f1", "notebook", "disk", "1000", "screen_size", "15"));
        assertU(adoc("id", "8", "f1", "notebook", "disk", "1199", "screen_size", "14.3"));
        assertU(adoc("id", "9", "f1", "notebook", "disk", "1201", "screen_size", "17"));
        assertU(adoc("id", "10", "f1", "notebook", "disk", "800", "screen_size", "11.7"));
        assertU(adoc("id", "11", "f1", "notebook", "disk", "1000", "screen_size", "11.7"));

        assertU(adoc("id", "12", "f1", "smartphone", "disk", "1000", "screen_size", "9"));
        assertU(adoc("id", "13", "f1", "smartphone", "disk", "1001", "screen_size", "9.1"));
        assertU(adoc("id", "14", "f1", "smartphone", "disk", "1500", "screen_size", "11.7"));

        assertU(adoc("id", "20", "f1", "10 zoll", "screen_size", "48.7", "fieldUnlimited", "-100000"));
        assertU(commit());
    }

    @Test
    public void testBoostingForExactMatchRange() {
        String q = "smartphone 9 zoll";

        SolrQueryRequest req = req("q", q,
                "sort", "id desc",
                DisMaxParams.QF, "f1",
                "fl", "id,score",
                DisMaxParams.MM, "100%",
                "uq.similarityScore", "off",
                "debugQuery", "on",
                "defType", "querqy_exact_match_range");

        assertQ("",
                req,
                "//result[@name='response' and @numFound='2']",
                "//result[@name='response']/doc[1]/str[@name='id'][text()='13']",
                "//result[@name='response']/doc[1]/float[@name='score'][text()='31.0']",
                "//result[@name='response']/doc[2]/str[@name='id'][text()='12']",
                "//result[@name='response']/doc[2]/float[@name='score'][text()='31.0']"
        );
        req.close();
    }

    @Test
    public void testBoostingForExactMatchRangeAcrossUnits() {
        String q = "smartphone 9 zoll 1000gb";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "fl", "id,score",
                DisMaxParams.MM, "100%",
                "uq.similarityScore", "off",
                "debugQuery", "on",
                "defType", "querqy_exact_match_range");

        assertQ("",
                req,
                "//result[@name='response' and @numFound='2']",
                "//result[@name='response']/doc[1]/str[@name='id'][text()='12']",
                "//result[@name='response']/doc[1]/float[@name='score'][text()='61.0']",
                "//result[@name='response']/doc[2]/str[@name='id'][text()='13']",
                "//result[@name='response']/doc[2]/float[@name='score'][text()='51.0']"
        );
        req.close();
    }

    @Test
    public void testUnlimitedRange() {
        String q = "55unitUnlimited";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "fl", "id,score",
                DisMaxParams.MM, "100%",
                "uq.similarityScore", "off",
                "defType", "querqy_standard");
                

        assertQ("", req,
                "//result[@name='response' and @numFound='3']",
                "//str[@name='id'][contains(.,'5')]",
                "//str[@name='id'][contains(.,'6')]",
                "//str[@name='id'][contains(.,'20')]"

        );
        req.close();
    }

    @Test
    public void testNumberUnitOnlyQuery() {
        String q = "55 zoll";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "fl", "id,score",
                DisMaxParams.MM, "100%",
                "uq.similarityScore", "off",
                "defType", "querqy_standard");
                

        assertQ("", req, "//result[@name='response' and @numFound='4']");
        req.close();
    }

    @Test
    public void testMatchAllQuery() {
        String q = "*:*";
        SolrQueryRequest req = req("q", q);
        assertQ("", req, "//result[@name='response' and @numFound='15']");
        req.close();
    }

    @Test
    public void testBoostingForMultipleNumberUnitInputs() {
        String q = "tv 200 cm 2 cm";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "fl", "id,score",
                DisMaxParams.MM, "100%",
                "uq.similarityScore", "off",
                "defType", "querqy_standard");

        assertQ("",
                req,
                "//result[@name='response']/doc[1]/str[@name='id'][text()='1']",
                "//result[@name='response']/doc[1]/float[@name='score'][text()='601.0']",
                "//result[@name='response']/doc[2]/str[@name='id'][text()='2']",
                "//result[@name='response']/doc[2]/float[@name='score'][text()='476.0']"
        );
        req.close();
    }

    @Test
    public void testBoostingForMultipleNumberUnitInputsAcrossUnits() {
        String q = "notebook 14 zoll 1tb";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "fl", "id,score",
                DisMaxParams.MM, "100%",
                "uq.similarityScore", "off",
                "defType", "querqy_standard");
                
        assertQ("",
                req,
                "//result[@name='response']/doc[1]/str[@name='id'][text()='7']",
                "//result[@name='response']/doc[1]/float[@name='score'][text()='22.0']",
                "//result[@name='response']/doc[2]/str[@name='id'][text()='8']",
                "//result[@name='response']/doc[2]/float[@name='score'][text()='20.0']",
                "//result[@name='response']/doc[3]/str[@name='id'][text()='6']",
                "//result[@name='response']/doc[3]/float[@name='score'][text()='19.0']",
                "//result[@name='response']/doc[4]/str[@name='id'][text()='11']",
                "//result[@name='response']/doc[4]/float[@name='score'][text()='18.0']",
                "//result[@name='response']/doc[5]/str[@name='id'][text()='10']",
                "//result[@name='response']/doc[5]/float[@name='score'][text()='13.0']"
        );
        req.close();
    }

    @Test
    public void testBoostingForSingleNumberUnitInputAndSingleUnitConfig() {
        String q = "notebook 15 zoll";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "fl", "id,score",
                DisMaxParams.MM, "100%",
                "uq.similarityScore", "off",
                "defType", "querqy_standard");
                

        assertQ("",
                req,
                "//result[@name='response']/doc[1]/str[@name='id'][text()='7']",
                "//result[@name='response']/doc[1]/float[@name='score'][text()='26.0']",
                "//result[@name='response']/doc[2]/str[@name='id'][text()='6']",
                "//result[@name='response']/doc[2]/float[@name='score'][text()='20.0']",
                "//result[@name='response']/doc[3]/str[@name='id'][text()='8']",
                "//result[@name='response']/doc[3]/float[@name='score'][text()='19.0']",
                "//result[@name='response']/doc[4]/str[@name='id'][text()='9']",
                "//result[@name='response']/doc[4]/float[@name='score'][text()='14.0']"
        );
        req.close();
    }

    @Test
    public void testFilteringForSingleNumberUnitInputAndSingleUnitConfig() {
        String q = "tv 55 zoll";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "fl", "id",
                DisMaxParams.MM, "100%",
                "echoParams", "all",
                "defType", "querqy_standard");
                

        assertQ("",
                req,
                "//result[@name='response' and @numFound='3']",
                "//str[@name='id'][contains(.,'1')]",
                "//str[@name='id'][contains(.,'2')]",
                "//str[@name='id'][contains(.,'3')]"
        );
        req.close();
    }

    @Test
    public void testFilteringForMultipleNumberUnitInputs() {
        String q = "tv 200 cm 2 cm";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "fl", "id",
                DisMaxParams.MM, "100%",
                "echoParams", "all",
                "defType", "querqy_standard");
                

        assertQ("",
                req,
                "//result[@name='response' and @numFound='2']",
                "//str[@name='id'][contains(.,'1')]",
                "//str[@name='id'][contains(.,'2')]"
        );
        req.close();
    }

    @Test
    public void testFilteringForMultipleNumberUnitInputsAcrossUnits() {
        String q;
        q = "tv 55 zoll 20 mm";

        SolrQueryRequest req;
        req = req("q", q,
                DisMaxParams.QF, "f1",
                "fl", "id",
                DisMaxParams.MM, "100%",
                "echoParams", "all",
                "defType", "querqy_standard");
                

        assertQ("",
                req,
                "//result[@name='response' and @numFound='2']",
                "//str[@name='id'][contains(.,'1')]",
                "//str[@name='id'][contains(.,'2')]"
        );

        q = "tv 35 zoll 20 mm";
        req = req("q", q,
                DisMaxParams.QF, "f1",
                "fl", "id",
                DisMaxParams.MM, "100%",
                "echoParams", "all",
                "defType", "querqy_standard");
                

        assertQ("",
                req,
                "//result[@name='response' and @numFound='1']",
                "//str[@name='id'][contains(.,'4')]"
        );

        req.close();
    }

    @Test
    public void testFilteringForSingleNumberUnitInputAndMultipleUnitConfig() {
        String q = "tv 210 cm";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "100%",
                "echoParams", "all",
                "defType", "querqy_standard");
                

        assertQ("",
                req,
                "//result[@name='response' and @numFound='2']",
                "//str[@name='id'][contains(.,'1')]",
                "//str[@name='id'][contains(.,'2')]"
        );
        req.close();
    }

    @Test
    public void testFilteringForSingleNumberUnitInputAndMultipleUnitConfig2() {
        String q = "tv 120 cm";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "100%",
                "echoParams", "all",
                "defType", "querqy_standard");

        assertQ("",
                req,
                "//result[@name='response' and @numFound='4']",
                "//str[@name='id'][contains(.,'1')]",
                "//str[@name='id'][contains(.,'2')]",
                "//str[@name='id'][contains(.,'3')]",
                "//str[@name='id'][contains(.,'4')]"
        );
        req.close();
    }
}
