package querqy.solr.contrib;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class ReplaceRewriterFactoryTest extends SolrTestCaseJ4 {

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("contrib/solrconfig-replace.xml", "schema.xml");
    }

    @Test
    public void testMatchAllQuery() {
        String q = "*:*";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy_defaults",
                "debugQuery", "on"
        );

        assertQ("", req, "//result[@name='response' and @numFound='0']");
        req.close();
    }


    @Test
    public void testDefaults() {
        String q = "a b d";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy_defaults",
                "debugQuery", "on"
        );

        assertQ("Replace rules",
                req,
                "//str[@name='parsedquery_toString'][text() = 'f1:e f1:f f1:g']"
        );

        req.close();
    }

    @Test
    public void testEmptyQueryAfterRewriting() {
        String q;
        SolrQueryRequest req;

        q = "prefix1";
        req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy_commonrules",
                "debugQuery", "on"
        );

        assertQ("Replace rules", req, "//str[@name='parsedquery_toString'][text() = 'MatchNoDocsQuery(\"\")']");
        req.close();

        q = "suffix1";
        req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy_commonrules",
                "debugQuery", "on"
        );

        assertQ("Replace rules", req, "//str[@name='parsedquery_toString'][text() = 'MatchNoDocsQuery(\"\")']");
        req.close();

        q = "exactmatch1";
        req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy_commonrules",
                "debugQuery", "on"
        );

        assertQ("Replace rules", req, "//str[@name='parsedquery_toString'][text() = 'MatchNoDocsQuery(\"\")']");
        req.close();
    }

    @Test
    public void testOverlaps() {
        String q;
        SolrQueryRequest req;

        q = "ghij ghi gh klm kl k mn op qr s t uv w xy z";
        req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "on"
        );

        assertQ("Replace rules", req,
                "//str[@name='parsedquery_toString'][text() = 'f1:gh f1:g f1:jk f1:j f1:opq f1:t f1:tu']"
        );
        req.close();
    }

    @Test
    public void testMultipleSubsequentReplacements() {
        String q;
        SolrQueryRequest req;

        q = "b c d e h";
        req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "on"
        );

        assertQ("Replace rules", req,
                "//str[@name='parsedquery_toString'][text() = 'f1:a f1:b f1:c f1:d f1:e f1:f f1:g f1:h']"
        );
        req.close();
    }

    @Test
    public void testRuleCombinations() {
        String q;
        SolrQueryRequest req;

        q = "acdf";
        req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "on"
        );

        assertQ("Replace rules", req,
                "//str[@name='parsedquery_toString'][text() = 'f1:acdf']"
        );
        req.close();
    }

    @Test
    public void testMultipleOutputTermsForSuffixRule() {
        String q;
        SolrQueryRequest req;

        q = "mnopqr";
        req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "on"
        );

        assertQ("Replace rules", req,
                "//str[@name='parsedquery_toString'][text() = 'f1:mn f1:opqr']"
        );
        req.close();
    }

    @Test
    public void testMultipleOutputTermsForPrefixRule() {
        String q;
        SolrQueryRequest req;

        q = "qrstuvw";
        req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "on"
        );

        assertQ("Replace rules", req,
                "//str[@name='parsedquery_toString'][text() = 'f1:qrst f1:uvw']"
        );
        req.close();
    }


}
