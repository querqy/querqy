package querqy.solr;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class SkipUnknownRewriterTest extends SolrTestCaseJ4 {

    public void index() {

        assertU(adoc("id", "1", "f1", "a"));
        assertU(commit());

    }

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-skip-unknown-rewriter.xml", "schema.xml");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearIndex();
        index();
    }

    @Test
    public void testUnknownRewriterReturnsBadRequestByDefault() {

        String q = "a";

        try(SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy-default",
                PARAM_REWRITERS, "unknown_rewriter"
        )) {
           assertQEx("Unknown rewriter should trigger bad request", req, SolrException.ErrorCode.BAD_REQUEST);
        }

    }

    @Test
    public void testUnknownRewriterReturnsBadRequestIfConfigured() {

        String q = "a";

        try(SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy-dont-skip",
                PARAM_REWRITERS, "unknown_rewriter"
        )) {
            assertQEx("Unknown rewriter should trigger bad request", req, SolrException.ErrorCode.BAD_REQUEST);
        }

    }

    @Test
    public void testUnknownRewriterIsSkipped() {

        String q = "a";

        try(SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy-skip",
                PARAM_REWRITERS, "unknown_rewriter"
        )) {
            assertQ("Unknown rewriter", req, "//result[@name='response' and @numFound='1']");
        }

    }

}
