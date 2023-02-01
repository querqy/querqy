package querqy.solr;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.deleteRewriter;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.rewrite.lookup.preprocessing.LookupPreprocessorType;
import querqy.solr.rewriter.commonrules.CommonRulesConfigRequestBuilder;

@SolrTestCaseJ4.SuppressSSL
public class QuerqyRewriterRequestHandlerStandaloneTest extends SolrTestCaseJ4 {

    public void index() {

        assertU(adoc("id", "1", "f1", "a"));
        assertU(adoc("id", "2", "f2", "b"));
        assertU(adoc("id", "3", "f2", "c"));
        assertU(commit());
    }


    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig.xml", "schema.xml");


    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearIndex();
        index();
    }


    @Test
    public void testSaveRewriter() {

        final CommonRulesConfigRequestBuilder builder = new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: b");
        withCommonRulesRewriter(h.getCore(), "rewriter_test_save", builder);

        String q = "a";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "1",
                QueryParsing.OP, "OR",
                "defType", "querqy",
                PARAM_REWRITERS, "rewriter_test_save"
        );

        assertQ("Rewriter not saved",
                req,
                "//result[@name='response' and @numFound='2']"

        );

        req.close();
    }

    @Test
    public void testGetConfig() {

        final String rewriterName = "conf_common_rules";
        final CommonRulesConfigRequestBuilder builder = new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: b").lookupPreprocessorType(LookupPreprocessorType.GERMAN);
        withCommonRulesRewriter(h.getCore(), rewriterName, builder);



        try (final SolrQueryRequest req = req("qt", "/querqy/rewriter/" + rewriterName )) {

            assertQ("Rewriter config not found",
                    req,
                    "//lst[@name='rewriter']/str[@name='id'][text()='" + rewriterName + "']",
                    "//lst[@name='rewriter']/str[@name='path'][text()='/querqy/rewriter/" + rewriterName + "']",
                    "//lst[@name='rewriter']/lst[@name='definition']/str[@name='class']" +
                            "[text()='querqy.solr.rewriter.commonrules.CommonRulesRewriterFactory']",
                    "//lst[@name='rewriter']/lst[@name='definition']/lst[@name='config']/str[@name='lookupPreprocessor']" +
                            "[text()='german']",
                    "//lst[@name='rewriter']/lst[@name='definition']/lst[@name='config']/str[@name='rules']" +
                            "[contains(.,'SYNONYM: b')]"
            );

        }

    }

    @Test
    public void testListConfigs() {

        final String rewriterName1 = "rewriter1";
        final CommonRulesConfigRequestBuilder builder1 = new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: b").lookupPreprocessorType(LookupPreprocessorType.NONE);
        withCommonRulesRewriter(h.getCore(), rewriterName1, builder1);

        final String rewriterName2 = "rewriter2";
        final CommonRulesConfigRequestBuilder builder2 = new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: b").lookupPreprocessorType(LookupPreprocessorType.NONE);
        withCommonRulesRewriter(h.getCore(), rewriterName2, builder2);



        try (final SolrQueryRequest req = req("qt", "/querqy/rewriter")) {

            assertQ("Rewriter config not found",
                    req,
                    "//lst[@name='rewriters']/lst[@name='" + rewriterName1 + "']/str[@name='id'][text()='" +
                            rewriterName1 + "']",
                    "//lst[@name='rewriters']/lst[@name='" + rewriterName1 + "']/str[@name='path'][text()='" +
                            "/querqy/rewriter/" + rewriterName1 + "']",
                    "//lst[@name='rewriters']/lst[@name='" + rewriterName2 + "']/str[@name='id'][text()='" +
                            rewriterName2 + "']",
                    "//lst[@name='rewriters']/lst[@name='" + rewriterName2 + "']/str[@name='path'][text()='" +
                            "/querqy/rewriter/" + rewriterName2 + "']"

            );

        }

    }



    @Test
    public void testUpdateRewriter() {

        final CommonRulesConfigRequestBuilder builder = new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: b");
        withCommonRulesRewriter(h.getCore(), "rewriter_update", builder);

        String q = "a";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "1",
                QueryParsing.OP, "OR",
                "defType", "querqy",
                PARAM_REWRITERS, "rewriter_update"
        );

        assertQ("Rewriter not saved",
                req,
                "//result[@name='response' and @numFound='2']"

        );

        req.close();

        withCommonRulesRewriter(h.getCore(), "rewriter_update", new CommonRulesConfigRequestBuilder()
                .rules(""));

        req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "1",
                QueryParsing.OP, "OR",
                "defType", "querqy",
                PARAM_REWRITERS, "rewriter_update"
        );

        assertQ("Rewriter not updated",
                req,
                "//result[@name='response' and @numFound='1']"

        );

        req.close();


    }


    @Test
    public void testUnknownRewriterReturnsBadRequest() {

        String q = "a";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "1",
                QueryParsing.OP, "OR",
                "defType", "querqy",
                PARAM_REWRITERS, "unknown_rewriter"
        );

        assertQEx("Unknown rewriter should trigger bad request", req, SolrException.ErrorCode.BAD_REQUEST);
    }

    @Test
    public void testRewriterCombination() {

        final CommonRulesConfigRequestBuilder builder = new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: b");
        withCommonRulesRewriter(h.getCore(), "rewriter_combi_1", builder);

        withCommonRulesRewriter(h.getCore(), "rewriter_combi_2", new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: c"));

        String q = "a";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "1",
                QueryParsing.OP, "OR",
                "defType", "querqy",
                PARAM_REWRITERS, "rewriter_combi_1,rewriter_combi_2"
        );

        assertQ("Rewriter combination doesn't work",
                req,
                "//result[@name='response' and @numFound='3']"

        );

        req.close();
    }


    @Test
    public void testDeleteRewriter() {

        final CommonRulesConfigRequestBuilder builder = new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: b");
        withCommonRulesRewriter(h.getCore(), "rewriter_to_be_deleted", builder);

        String q = "a";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "1",
                QueryParsing.OP, "OR",
                "defType", "querqy",
                PARAM_REWRITERS, "rewriter_to_be_deleted"
        );

        assertQ("Rewriter not saved",
                req,
                "//result[@name='response' and @numFound='2']"

        );

        req.close();

        deleteRewriter(h.getCore(), "rewriter_to_be_deleted");

        req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "1",
                QueryParsing.OP, "OR",
                "defType", "querqy",
                PARAM_REWRITERS, "rewriter_to_be_deleted"
        );

        assertQEx("Unknown rewriter should trigger bad request", req, SolrException.ErrorCode.BAD_REQUEST);

        req.close();
    }


}
