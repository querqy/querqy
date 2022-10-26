package querqy.solr;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class InfoLoggingTest extends SolrTestCaseJ4 {

    private final static String REWRITERS = "common1,common2";
    private static final String REWRITE_CHAIN_PATH = "//lst[@name='querqy.rewriteLogging']/arr[@name='rewriteChainLogging']/lst";
    private static final String ACTIONS_PATH = REWRITE_CHAIN_PATH + "/arr[@name='actions']/lst";


    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-infoLogging.xml", "schema.xml");
        withCommonRulesRewriter(h.getCore(), "common1", "configs/commonrules/rules-infoLogging1.txt");
        withCommonRulesRewriter(h.getCore(), "common2", "configs/commonrules/rules-infoLogging2.txt");
    }

    @Test
    public void testThatOnlyIdAreReturnedForGivenParam() {

        String q = "c";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                RewriteLoggingParameter.REWRITE_LOGGING_PARAM_KEY, RewriteLoggingParameter.REWRITER_ID.getValue(),                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Logged ID is missing",
                req,
                REWRITE_CHAIN_PATH + "/str[@name='rewriterId' and text()='common1']",
                REWRITE_CHAIN_PATH + "/str[@name='rewriterId' and text()='common2']",
                "count(" + REWRITE_CHAIN_PATH + "/arr[@name='actions']) = 0"
        );

        req.close();
    }

    @Test
    public void testThatLogsAreReturnedForAllMatchingRules() {

        String q = "m";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                RewriteLoggingParameter.REWRITE_LOGGING_PARAM_KEY, RewriteLoggingParameter.DETAILS.getValue(),
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Logging multiple logs for same input false",
                req,
                ACTIONS_PATH + "/str[@name='message' and text()='LOG 1 for m']",
                ACTIONS_PATH + "/str[@name='message' and text()='LOG 2 for m']",
                "count(" + ACTIONS_PATH + "/str) = 2"
        );

        req.close();
    }

    @Test
    public void testThatLogsAreReturnedForAllMatchingInputsOfTheSameRewriter() {

        String q = "a c d k m";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                RewriteLoggingParameter.REWRITE_LOGGING_PARAM_KEY, RewriteLoggingParameter.DETAILS.getValue(),
                "defType", "querqy",
                PARAM_REWRITERS, "common1"
        );

        assertQ("Logging multiple logs for same input false",
                req,
                ACTIONS_PATH + "/str[@name='message' and text()='log msg 1 of input a']",
                ACTIONS_PATH + "/str[@name='message' and text()='ID 1']",
                ACTIONS_PATH + "/str[@name='message' and text()='LOG for k']",
                ACTIONS_PATH + "/str[@name='message' and text()='d#2']",
                ACTIONS_PATH + "/str[@name='message' and text()='LOG 1 for m']",
                ACTIONS_PATH + "/str[@name='message' and text()='LOG 2 for m']",
                "count(" + ACTIONS_PATH + ") = 6"
        );

        req.close();
    }

    public void testThatLogsAreReturnedForAllRewriters() {
        String q = "k";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                QuerqyDismaxParams.INFO_LOGGING, "on",
                RewriteLoggingParameter.REWRITE_LOGGING_PARAM_KEY, RewriteLoggingParameter.DETAILS.getValue(),
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Logging multiple logs for same input false",
                req,
                REWRITE_CHAIN_PATH + "/str[@name='rewriterId' and text()='common1']",
                REWRITE_CHAIN_PATH + "/str[@name='rewriterId' and text()='common2']"
        );
    }

    public void testThatDetailedLogsAreReturnedForGivenParam() {

        String q = "k";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                RewriteLoggingParameter.REWRITE_LOGGING_PARAM_KEY, RewriteLoggingParameter.DETAILS.getValue(),
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Logging multiple logs for same input false",
                req,
                REWRITE_CHAIN_PATH + "/str[@name='rewriterId' and text()='common1']",
                ACTIONS_PATH + "/str[@name='message' and text()='LOG for k']",
                ACTIONS_PATH + "/lst[@name='match']/str[@name='term' and text()='k']",
                ACTIONS_PATH + "/lst[@name='match']/str[@name='type' and text()='exact']",
                ACTIONS_PATH + "/arr[@name='instructions']/lst/str[@name='type' and text()='synonym']",
                ACTIONS_PATH + "/arr[@name='instructions']/lst/str[@name='value' and text()='h']"
        );

        req.close();
    }

    public void testThatLogOutputIsTurnedOffByDefault() {

        String q = "k";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Logging multiple logs for same input false",
                req,
                "count(" + REWRITE_CHAIN_PATH + ") = 0"
        );

        req.close();
    }

    public void testThatThereIsNoLogOutputIfParamIsOff() {

        String q = "k";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                RewriteLoggingParameter.REWRITE_LOGGING_PARAM_KEY, RewriteLoggingParameter.OFF.getValue(),
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Logging multiple logs for same input false",
                req,
                "count(" + REWRITE_CHAIN_PATH + ") = 0"
        );

        req.close();
    }

}
