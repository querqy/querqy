package querqy.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import querqy.lucene.rewrite.infologging.Sink;
import querqy.rewrite.SearchEngineRequestAdapter;

import java.util.List;
import java.util.Map;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.RewriteLoggingParameters.PARAM_REWRITE_LOGGING_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;

@SolrTestCaseJ4.SuppressSSL
public class CustomInfoLoggingSinkTest extends SolrTestCaseJ4 {

    private static final String REWRITE_CHAIN_PATH = "//lst[@name='querqyRewriteLogging']/arr[@name='rewriteChainLogging']/lst";


    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-infoLogging.xml", "schema.xml");
        withCommonRulesRewriter(h.getCore(), "common1",
                "configs/commonrules/rules-infoLogging1.txt", "response");
        withCommonRulesRewriter(h.getCore(), "common2",
                "configs/commonrules/rules-infoLogging1.txt", "customSink");

    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        CustomSink.caughtMessage = null;
        CustomSink.caughtRewriterId = null;
    }

    @After
    public void resetVars() {
        CustomSink.caughtMessage = null;
        CustomSink.caughtRewriterId = null;
    }


    public void testThatRewritersDirectToTheRightSink() {
        String q = "k";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                QuerqyDismaxParams.INFO_LOGGING, "on",
                RewriteLoggingParameters.REWRITE_LOGGING_PARAM_KEY, RewriteLoggingParameters.DETAILS.getValue(),
                PARAM_REWRITE_LOGGING_REWRITERS, "common1,common2",
                "defType", "querqy",
                PARAM_REWRITERS, "common1,common2"
        );

        assertQ("Expect only common1 to log to response",
                req,
                REWRITE_CHAIN_PATH + "/str[@name='rewriterId' and text()='common1']",
                "count(" + REWRITE_CHAIN_PATH + "/str[@name='rewriterId' and text()='common2']) = 0"

        );

        assertEquals("common2", CustomSink.caughtRewriterId);
        assertTrue(CustomSink.caughtMessage instanceof List);
        List<?> logs = (List) CustomSink.caughtMessage;
        assertTrue(logs.get(0) instanceof Map);
        assertEquals("LOG for k", ((Map) logs.get(0)).get("message"));

    }

    public static class CustomSink implements Sink {

        public static Object caughtMessage = null;
        public static Object caughtRewriterId = null;


        @Override
        public void log(final Object message, final String rewriterId,
                        final SearchEngineRequestAdapter searchEngineRequestAdapter) {

            // make sure we are being called only once (i.e. for the common2 rewriter)
            assertNull(caughtMessage);

            caughtMessage = message;
            caughtRewriterId = rewriterId;

        }

        @Override
        public void endOfRequest(final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        }
    }

}
