package querqy.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class CommonRulesBooleanInputTest extends SolrTestCaseJ4 {

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-booleaninput.xml", "schema.xml");
        addDocs();
    }

    private static void addDocs() {
        assertU(adoc("id", "1", "f1", "a b c", "f2", "d"));
        assertU(adoc("id", "2", "f1", "a b c", "f2", "e"));
        assertU(adoc("id", "3", "f1", "a b c", "f2", "f"));
        assertU(adoc("id", "4", "f1", "gh i", "f2", "j"));
        assertU(adoc("id", "5", "f1", "gh i", "f2", "k"));
        assertU(adoc("id", "6", "f1", "l m", "f2", "n"));
        assertU(adoc("id", "7", "f1", "l m", "f2", "o"));

        assertU(commit());
    }

    private String[] createParams(String q) {
        return new String[]{
                "q", q,
                DisMaxParams.QF, "f1",
                "fl", "id,score",
                DisMaxParams.MM, "100%",
                "debugQuery", "on",
                "defType", "querqy"
        };
    }

    @Test
    public void testFilteringForSimpleBooleanInput() {
        SolrQueryRequest req;

        req = req(createParams("a b"));
        assertQ("", req,
                "//result[@name='response' and @numFound='1']");
        req.close();

        req = req(createParams("c"));
        assertQ("", req,
                "//result[@name='response' and @numFound='1']");
        req.close();

        req = req(createParams("a"));
        assertQ("", req,
                "//result[@name='response' and @numFound='3']");
        req.close();

        req = req(createParams("b"));
        assertQ("", req,
                "//result[@name='response' and @numFound='3']");
        req.close();
    }

    @Test
    public void testFilteringForPrefixInput() {
        SolrQueryRequest req;

        req = req(createParams("gh i"));
        assertQ("", req,
                "//result[@name='response' and @numFound='1']");
        req.close();
    }

    @Test
    public void testFilteringBoundaryInput() {
        SolrQueryRequest req;

        req = req(createParams("l l m"));
        assertQ("", req,
                "//result[@name='response' and @numFound='1']");
        req.close();

        req = req(createParams("m l l"));
        assertQ("", req,
                "//result[@name='response' and @numFound='2']");
        req.close();

        req = req(createParams("l m m"));
        assertQ("", req,
                "//result[@name='response' and @numFound='1']");
        req.close();

        req = req(createParams("m m l"));
        assertQ("", req,
                "//result[@name='response' and @numFound='2']");
        req.close();

        req = req(createParams("m m m"));
        assertQ("", req,
                "//result[@name='response' and @numFound='1']");
        req.close();

        req = req(createParams("l l l"));
        assertQ("", req,
                "//result[@name='response' and @numFound='1']");
        req.close();

        req = req(createParams("m m m m"));
        assertQ("", req,
                "//result[@name='response' and @numFound='2']");
        req.close();

        req = req(createParams("l l l l"));
        assertQ("", req,
                "//result[@name='response' and @numFound='2']");
        req.close();

    }

}
