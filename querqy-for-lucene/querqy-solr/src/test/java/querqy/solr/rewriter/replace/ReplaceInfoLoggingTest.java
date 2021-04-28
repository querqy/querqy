package querqy.solr.rewriter.replace;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.solr.QuerqyDismaxParams;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withReplaceRewriter;

@SolrTestCaseJ4.SuppressSSL
public class ReplaceInfoLoggingTest extends SolrTestCaseJ4 {

    private final static String REWRITERS = "replace1,replace2";

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-replace-infoLogging.xml", "schema.xml");
        withReplaceRewriter(h.getCore(), "replace1", "configs/replace/replace-rules.txt");
        withReplaceRewriter(h.getCore(), "replace2", "configs/replace/replace-rules-defaults.txt");
    }

    @Test
    public void testThatTheLogPropertyIsReturned() {

        String q = "testword";

        SolrQueryRequest req = req("q", q,
                QuerqyDismaxParams.INFO_LOGGING, "on",
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Log property is missing",
                req,
                "count(//lst[@name='querqy.infoLog']/arr[@name='replace1']/lst/arr[@name='APPLIED_RULES']/str[1][text() = 'testword => [word]']) = 1"
        );

        req.close();
    }

    @Test
    public void testThatThereIsNoLogOutputIfThereIsNoMatch() {

        String q = "notpresent";

        SolrQueryRequest req = req("q", q,
                QuerqyDismaxParams.INFO_LOGGING, "on",
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("There should be no output for no match",
                req,
                "count(//lst[@name='querqy.infoLog']/arr[@name='replace1']/lst/arr[@name='APPLIED_RULES']) = 0"
        );

        req.close();
    }

    @Test
    public void testThatLogsAreReturnedForAllRewriters() {

        String q = "wordtest testword";

        SolrQueryRequest req = req("q", q,
                QuerqyDismaxParams.INFO_LOGGING, "on",
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Rules for all replace rewriters should be returned",
                req,
                "count(//lst[@name='querqy.infoLog']/arr[@name='replace1']/lst/arr[@name='APPLIED_RULES']/str[1][text() = 'testword => [word]']) = 1",
                "count(//lst[@name='querqy.infoLog']/arr[@name='replace2']/lst/arr[@name='APPLIED_RULES']/str[1][text() = 'wordtest => [test]']) = 1"
                );

        req.close();
    }

    public void testThatLogsAreReturnedForAllMatchingRules() {

        String q = "testword superword wordword replaceme wordtest testtesttest testtest";

        SolrQueryRequest req = req("q", q,
                QuerqyDismaxParams.INFO_LOGGING, "on",
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Multiple rules for all replace rewriters should be returned",
                req,
                "count(//lst[@name='querqy.infoLog']/arr[@name='replace1']/lst/arr[@name='APPLIED_RULES']/str[1][text() = 'replaceme => [replaced]']) = 1",
                "count(//lst[@name='querqy.infoLog']/arr[@name='replace1']/lst/arr[@name='APPLIED_RULES']/str[2][text() = 'wordword,superword,testword => [word]']) = 1",
                "count(//lst[@name='querqy.infoLog']/arr[@name='replace2']/lst/arr[@name='APPLIED_RULES']/str[1][text() = 'testtesttest,testtest => [tested]']) = 1",
                "count(//lst[@name='querqy.infoLog']/arr[@name='replace2']/lst/arr[@name='APPLIED_RULES']/str[2][text() = 'wordtest => [test]']) = 1"
        );

        req.close();
    }

    @Test
    public void testThatLogsAreReturnedForAllMatchingInputsOfTheSameRewriter() {

        String q = "testword superword replaceme";

        SolrQueryRequest req = req("q", q,
                QuerqyDismaxParams.INFO_LOGGING, "on",
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Multiple rules for one replace rewriter should be returned",
                req,
                "count(//lst[@name='querqy.infoLog']/arr[@name='replace1']/lst/arr[@name='APPLIED_RULES']/str[1][text() = 'replaceme => [replaced]']) = 1",
                "count(//lst[@name='querqy.infoLog']/arr[@name='replace1']/lst/arr[@name='APPLIED_RULES']/str[2][text() = 'superword,testword => [word]']) = 1"
                );

        req.close();
    }

    public void testThatLogOutputIsTurnedOffByDefault() {

        String q = "testword";

        SolrQueryRequest req = req("q", q,
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Logging should be off by default",
                req,
                "count(//lst[@name='querqy.infoLog']/arr) = 0"
        );

        req.close();
    }

    public void testThatThereIsNoLogOutputIfParamIsOff() {

        String q = "testword";

        SolrQueryRequest req = req("q", q,
                QuerqyDismaxParams.INFO_LOGGING, "off",
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Logging should be off by param",
                req,
                "count(//lst[@name='querqy.infoLog']/arr) = 0"
        );

        req.close();
    }

}
