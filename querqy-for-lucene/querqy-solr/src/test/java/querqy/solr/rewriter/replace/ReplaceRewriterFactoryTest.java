package querqy.solr.rewriter.replace;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;
import static querqy.solr.StandaloneSolrTestSupport.withRewriter;
import static querqy.solr.rewriter.replace.ReplaceRewriterFactory.KEY_CONFIG_RULES;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.solr.StandaloneSolrTestSupport;
import querqy.solr.rewriter.replace.ReplaceRewriterFactory;

import java.util.HashMap;
import java.util.Map;

@SolrTestCaseJ4.SuppressSSL
public class ReplaceRewriterFactoryTest extends SolrTestCaseJ4 {

    @BeforeClass
    public static void beforeTests() throws Exception {

        initCore("solrconfig.xml", "schema.xml");

        final Map<String, Object> config = new HashMap<>();
        config.put(KEY_CONFIG_RULES,
                StandaloneSolrTestSupport.resourceToString("configs/replace/replace-rules-defaults.txt"));
        withRewriter(h.getCore(), "replace_defaults", ReplaceRewriterFactory.class, config);

        final Map<String, Object> config2 = new HashMap<>();
        config2.put(KEY_CONFIG_RULES,
                StandaloneSolrTestSupport.resourceToString("configs/replace/replace-rules.txt"));
        withRewriter(h.getCore(), "replace", ReplaceRewriterFactory.class, config2);

        withCommonRulesRewriter(h.getCore(), "common_rules", "configs/commonrules/replace-commonrules.txt");

    }

    @Test
    public void testMatchAllQuery() {
        String q = "*:*";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, "replace_defaults"
        );

        assertQ("", req, "//result[@name='response' and @numFound='0']");
        req.close();
    }


    @Test
    public void testDefaults() {
        String q = "a b d";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, "replace_defaults"
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
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, "replace,common_rules"
        );

        assertQ("Replace rules", req, "//str[@name='parsedquery_toString'][text() = 'MatchNoDocsQuery(\"\")']");
        req.close();

        q = "suffix1";
        req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, "replace,common_rules"
        );

        assertQ("Replace rules", req, "//str[@name='parsedquery_toString'][text() = 'MatchNoDocsQuery(\"\")']");
        req.close();

        q = "exactmatch1";
        req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, "replace,common_rules"
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
                "debugQuery", "on",
                PARAM_REWRITERS, "replace"
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
                "debugQuery", "on",
                PARAM_REWRITERS, "replace"
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
                "debugQuery", "on",
                PARAM_REWRITERS, "replace"
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
                "debugQuery", "on",
                PARAM_REWRITERS, "replace"
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
                "debugQuery", "on",
                PARAM_REWRITERS, "replace"
        );

        assertQ("Replace rules", req,
                "//str[@name='parsedquery_toString'][text() = 'f1:qrst f1:uvw']"
        );
        req.close();
    }


}
