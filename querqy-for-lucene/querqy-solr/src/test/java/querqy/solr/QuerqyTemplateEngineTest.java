package querqy.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;

@SolrTestCaseJ4.SuppressSSL
public class QuerqyTemplateEngineTest extends SolrTestCaseJ4 {

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig.xml", "schema.xml");

        withCommonRulesRewriter(h.getCore(), "r1",
                "configs/commonrules/rules-templates.txt");

        addDocs();
    }


    private static void addDocs() {
        assertU(adoc("id", "1", "f1", "tv"));
        assertU(adoc("id", "2", "f1", "television"));
        assertU(adoc("id", "3", "f1", "smartphone"));
        assertU(commit());
    }

    @Test
    public void testBoostByIdTemplate() {
        String q = "smartphone";
        SolrQueryRequest req = req("q", q,
                "sort", "id desc",
                DisMaxParams.QF, "f1",
                "fl", "id,score",
                PARAM_REWRITERS, "r1",
                DisMaxParams.MM, "100%",
                "uq.similarityScore", "off",
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

    @Test
    public void testTwoSidedSynonymTemplate() {
        String q = "tv";
        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "fl", "id,score",
                PARAM_REWRITERS, "r1",
                DisMaxParams.MM, "100%",
                "debugQuery", "on",
                "defType", "querqy");

        assertQ("",
                req,
                "//result[@name='response' and @numFound='2']"
        );
        req.close();


        q = "television";
        req = req("q", q,
                DisMaxParams.QF, "f1",
                "fl", "id,score",
                PARAM_REWRITERS, "r1",
                DisMaxParams.MM, "100%",
                "debugQuery", "on",
                "defType", "querqy");

        assertQ("",
                req,
                "//result[@name='response' and @numFound='2']"
        );
        req.close();
    }


}
