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
                "debugQuery", "true",
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ(
                req,
                "//lst[@name='debug']/lst[@name='querqy']/str[@name='parser' and text()='querqy.parser.WhiteSpaceQuerqyParser']",
                "//lst[@name='debug']/lst[@name='querqy']/lst[@name='rewrite']/arr[@name='rewriteChain']"
        );

        req.close();
    }

    @Test
    public void testThatThereIsNoLogOutputIfThereIsNoMatch() {

        String q = "notpresent";

        SolrQueryRequest req = req("q", q,
                "debugQuery", "true",
                "defType", "querqy",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("There should be no output for no match",
                req,
                "count(//lst[@name='debug']/arr[@name='querqy.rewrite']) = 0"
        );

        req.close();
    }
}
