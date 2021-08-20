package querqy.solr.rewriter.replace;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.solr.QuerqyDismaxParams;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withReplaceRewriter;

@SolrTestCaseJ4.SuppressSSL
public class ReplaceDebugOutputTest extends SolrTestCaseJ4 {

    private final static String REWRITERS = "replace1,replace2";

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-replace-infoLogging.xml", "schema.xml");
        withReplaceRewriter(h.getCore(), "replace1", "configs/replace/replace-rules.txt");
        withReplaceRewriter(h.getCore(), "replace2", "configs/replace/replace-rules-defaults.txt");
    }

    @Test
    public void testThatAllReplacedWordsAreWrittenForSingleRewriter() {

        String q = "testword superword wordword";

        SolrQueryRequest req = req("q", q,
                QuerqyDismaxParams.INFO_LOGGING, "on",
                "debugQuery", "true",
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Log property is missing",
                req,
                "count(//lst[@name='debug']/arr[@name='querqy.rewrite']/str[1][text() = 'querqy.rewrite.contrib.ReplaceRewriter terms: word word word']) = 1"
        );

        req.close();
    }

    @Test
    public void testThatAllReplacedWordsAreWrittenForMultipleRewriters() {

        String q = "testword superword wordword testtesttest testtest";

        SolrQueryRequest req = req("q", q,
                QuerqyDismaxParams.INFO_LOGGING, "on",
                "debugQuery", "true",
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Log property is missing",
                req,
                "count(//lst[@name='debug']/arr[@name='querqy.rewrite']/str[1][text() = 'querqy.rewrite.contrib.ReplaceRewriter terms: word word word testtesttest testtest']) = 1",
                "count(//lst[@name='debug']/arr[@name='querqy.rewrite']/str[2][text() = 'querqy.rewrite.contrib.ReplaceRewriter terms: word word word tested tested']) = 1"
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
                "count(//lst[@name='debug']/arr[@name='querqy.rewrite']/str) = 0"
        );

        req.close();
    }
}
