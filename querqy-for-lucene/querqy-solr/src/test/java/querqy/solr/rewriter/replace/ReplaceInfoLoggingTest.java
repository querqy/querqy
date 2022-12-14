package querqy.solr.rewriter.replace;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.solr.RewriteLoggingParameters;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.RewriteLoggingParameters.PARAM_REWRITE_LOGGING_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withReplaceRewriter;

@SolrTestCaseJ4.SuppressSSL
public class ReplaceInfoLoggingTest extends SolrTestCaseJ4 {

    private final static String REWRITERS = "replace1,replace2";
    private static final String REWRITE_CHAIN_PATH = "//lst[@name='querqy_rewriteLogging']/arr[@name='rewriteChainLogging']/lst";
    private static final String ACTIONS_PATH = REWRITE_CHAIN_PATH + "/arr[@name='actions']/lst";

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-replace-infoLogging.xml", "schema.xml");
        withReplaceRewriter(h.getCore(), "replace1", "configs/replace/replace-rules.txt",
                "response");
        withReplaceRewriter(h.getCore(), "replace2", "configs/replace/replace-rules-defaults.txt",
                "response");
    }

    public void testThatDetailedLogsAreReturnedForGivenParam() {

        String q = "testword";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                RewriteLoggingParameters.REWRITE_LOGGING_PARAM_KEY, RewriteLoggingParameters.DETAILS.getValue(),
                PARAM_REWRITE_LOGGING_REWRITERS, REWRITERS,
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Logging multiple logs for same input false",
                req,
                REWRITE_CHAIN_PATH + "/str[@name='rewriterId' and text()='replace1']",
                ACTIONS_PATH + "/str[@name='message' and text()='testword => word']",
                ACTIONS_PATH + "/lst[@name='match']/str[@name='term' and text()='testword']",
                ACTIONS_PATH + "/lst[@name='match']/str[@name='type' and text()='exact']",
                ACTIONS_PATH + "/arr[@name='instructions']/lst/str[@name='type' and text()='replace']",
                ACTIONS_PATH + "/arr[@name='instructions']/lst/str[@name='value' and text()='word']"
        );

        req.close();
    }


    @Test
    public void testThatThereIsNoLogOutputIfThereIsNoMatch() {

        String q = "notpresent";

        SolrQueryRequest req = req("q", q,
                RewriteLoggingParameters.REWRITE_LOGGING_PARAM_KEY, RewriteLoggingParameters.DETAILS.getValue(),
                PARAM_REWRITE_LOGGING_REWRITERS, REWRITERS,
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ(req,
                "count(" + REWRITE_CHAIN_PATH + ") = 0"
        );

        req.close();
    }

    @Test
    public void testThatLogsAreReturnedForAllRewriters() {

        String q = "wordtest testword";

        SolrQueryRequest req = req("q", q,
                RewriteLoggingParameters.REWRITE_LOGGING_PARAM_KEY, RewriteLoggingParameters.DETAILS.getValue(),
                PARAM_REWRITE_LOGGING_REWRITERS, REWRITERS,
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ(req,
                REWRITE_CHAIN_PATH + "/str[@name='rewriterId' and text()='replace1']",
                REWRITE_CHAIN_PATH + "/str[@name='rewriterId' and text()='replace2']"
        );

        req.close();
    }

    public void testThatLogOutputIsTurnedOffByDefault() {

        String q = "testword";

        SolrQueryRequest req = req("q", q,
                PARAM_REWRITE_LOGGING_REWRITERS, REWRITERS,
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ(req,
                "count(" + REWRITE_CHAIN_PATH + ") = 0"
        );

        req.close();
    }

    public void testThatThereIsNoLogOutputIfParamIsOff() {

        String q = "testword";

        SolrQueryRequest req = req("q", q,
                RewriteLoggingParameters.REWRITE_LOGGING_PARAM_KEY, RewriteLoggingParameters.OFF.getValue(),
                PARAM_REWRITE_LOGGING_REWRITERS, REWRITERS,
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ(req,
                "count(" + REWRITE_CHAIN_PATH + ") = 0"
        );

        req.close();
    }

}
