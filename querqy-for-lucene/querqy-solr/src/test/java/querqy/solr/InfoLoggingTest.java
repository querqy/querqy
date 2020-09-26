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

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-infoLogging.xml", "schema.xml");
        withCommonRulesRewriter(h.getCore(), "common1", "configs/commonrules/rules-infoLogging1.txt");
        withCommonRulesRewriter(h.getCore(), "common2", "configs/commonrules/rules-infoLogging2.txt");
    }

    @Test
    public void testThatTheLogPropertyIsReturned() {

        String q = "a x";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                QuerqyDismaxParams.INFO_LOGGING, "on",
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Log property is missing",
                req,
                "//lst[@name='querqy.infoLog']/arr[@name='common1']/lst/arr[@name='APPLIED_RULES']/" +
                        "str[text() = 'log msg 1 of input a']",
                "count(//lst[@name='querqy.infoLog']/arr[@name='common1']/lst/arr[@name='APPLIED_RULES']/" +
                        "str) = 1"
        );

        req.close();
    }

    @Test
    public void testThatThereIsNoLogOutputIfOneRewriterDoesntSupplyAMessage() {

        String q = "a x";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                QuerqyDismaxParams.INFO_LOGGING, "on",
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Empty log output for rewriter",
                req,
                "count(//lst[@name='querqy.infoLog']/arr[@name='common2']) = 0"
        );

        req.close();
    }

    @Test
    public void testThatTheIdIsReturnedIfThereIsNoLogProperty() {

        String q = "c";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                QuerqyDismaxParams.INFO_LOGGING, "on",
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Logged ID is missing",
                req,
                "//lst[@name='querqy.infoLog']/arr[@name='common1']/lst/arr[@name='APPLIED_RULES']/" +
                        "str[text() = 'ID 1']",
                "count(//lst[@name='querqy.infoLog']/arr[@name='common1']/lst/arr[@name='APPLIED_RULES']/" +
                        "str) = 1"
        );

        req.close();
    }

    @Test
    public void testThatMatchExpressionPlusOrdIsReturnedIfThereIsNeitherLogPropertyNorIdConfigured() {

        String q = "d";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                QuerqyDismaxParams.INFO_LOGGING, "on",
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Synthetic log message missing",
                req,
                "//lst[@name='querqy.infoLog']/arr[@name='common1']/lst/arr[@name='APPLIED_RULES']/" +
                        "str[text() = 'd#2']",
                "count(//lst[@name='querqy.infoLog']/arr[@name='common1']/lst/arr[@name='APPLIED_RULES']/" +
                        "str) = 1"
        );

        req.close();
    }

    @Test
    public void testThatLogPropertyIsReturnedEvenIfIdIsConfigured() {

        String q = "k";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                QuerqyDismaxParams.INFO_LOGGING, "on",
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Log not returned if ID is configured",
                req,
                "//lst[@name='querqy.infoLog']/arr[@name='common1']/lst/arr[@name='APPLIED_RULES']/" +
                        "str[text() = 'LOG for k']",
                "count(//lst[@name='querqy.infoLog']/arr[@name='common1']/lst/arr[@name='APPLIED_RULES']/" +
                        "str) = 1"
        );

        req.close();
    }


    @Test
    public void testThatLogsAreReturnedForAllMatchingRules() {

        String q = "m";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                QuerqyDismaxParams.INFO_LOGGING, "on",
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Logging multiple logs for same input false",
                req,
                "//lst[@name='querqy.infoLog']/arr[@name='common1']/lst/arr[@name='APPLIED_RULES']/" +
                        "str[text() = 'LOG 1 for m']",
                "//lst[@name='querqy.infoLog']/arr[@name='common1']/lst/arr[@name='APPLIED_RULES']/" +
                        "str[text() = 'LOG 2 for m']",
                "count(//lst[@name='querqy.infoLog']/arr[@name='common1']/lst/arr[@name='APPLIED_RULES']/" +
                        "str) = 2"
        );

        req.close();
    }

    @Test
    public void testThatLogsAreReturnedForAllMatchingInputsOfTheSameRewriter() {

        String q = "a c d k m";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                QuerqyDismaxParams.INFO_LOGGING, "on",
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Logging multiple logs for same input false",
                req,
                "//lst[@name='querqy.infoLog']/arr[@name='common1']/lst/arr[@name='APPLIED_RULES']/" +
                        "str[text() = 'log msg 1 of input a']",
                "//lst[@name='querqy.infoLog']/arr[@name='common1']/lst/arr[@name='APPLIED_RULES']/" +
                        "str[text() = 'ID 1']",
                "//lst[@name='querqy.infoLog']/arr[@name='common1']/lst/arr[@name='APPLIED_RULES']/" +
                        "str[text() = 'LOG for k']",
                "//lst[@name='querqy.infoLog']/arr[@name='common1']/lst/arr[@name='APPLIED_RULES']/" +
                        "str[text() = 'd#2']",
                "//lst[@name='querqy.infoLog']/arr[@name='common1']/lst/arr[@name='APPLIED_RULES']/" +
                        "str[text() = 'LOG 1 for m']",
                "//lst[@name='querqy.infoLog']/arr[@name='common1']/lst/arr[@name='APPLIED_RULES']/" +
                        "str[text() = 'LOG 2 for m']",
                "count(//lst[@name='querqy.infoLog']/arr[@name='common1']/lst/arr[@name='APPLIED_RULES']/" +
                        "str) = 6"
        );

        req.close();
    }

    public void testThatLogsAreReturnedForAllRewriters() {

        String q = "k";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                QuerqyDismaxParams.INFO_LOGGING, "on",
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Logging multiple logs for same input false",
                req,
                "//lst[@name='querqy.infoLog']/arr[@name='common1']/lst/arr[@name='APPLIED_RULES']/" +
                        "str[text() = 'LOG for k']",
                "//lst[@name='querqy.infoLog']/arr[@name='common2']/lst/arr[@name='APPLIED_RULES']/" +
                        "str[text() = 'k in rewriter 2']",
                "count(//lst[@name='querqy.infoLog']/arr[@name='common1']/lst/arr[@name='APPLIED_RULES']/" +
                        "str) = 1",
                "count(//lst[@name='querqy.infoLog']/arr[@name='common1']/lst/arr[@name='APPLIED_RULES']/" +
                        "str) = 1"
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
                "count(//lst[@name='querqy.infoLog']/arr) = 0"
        );

        req.close();
    }

    public void testThatThereIsNoLogOutputIfParamIsOff() {

        String q = "k";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                QuerqyDismaxParams.INFO_LOGGING, "off",
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Logging multiple logs for same input false",
                req,
                "count(//lst[@name='querqy.infoLog']/arr) = 0"
        );

        req.close();
    }

}
