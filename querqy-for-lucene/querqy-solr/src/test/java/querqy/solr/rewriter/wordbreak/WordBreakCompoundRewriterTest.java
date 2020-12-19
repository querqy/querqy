package querqy.solr.rewriter.wordbreak;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;
import static querqy.solr.StandaloneSolrTestSupport.withRewriter;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class WordBreakCompoundRewriterTest extends SolrTestCaseJ4 {

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig.xml", "schema.xml");
        withCommonRulesRewriter(h.getCore(), "common_rules_before_wordbreak",
                "configs/commonrules/rules-before-wordbreaks.txt");

        final Map<String, Object> config = new HashMap<>();
        config.put("dictionaryField", "f1");
        config.put("reverseCompoundTriggerWords", Arrays.asList("für", "aus"));
        final Map<String, Object> decompoundConf = new HashMap<>();
        decompoundConf.put("verifyCollation", true);
        config.put("decompound", decompoundConf);
        config.put("lowerCaseInput", true);
        withRewriter(h.getCore(), "word_break", WordBreakCompoundRewriterFactory.class, config);

        final Map<String, Object> configDe = new HashMap<>(config);
        configDe.put("morphology", "GERMAN");
        withRewriter(h.getCore(), "word_break_de", WordBreakCompoundRewriterFactory.class, configDe);

        final Map<String, Object> configNoLowercaseNoVerify = new HashMap<>();
        configNoLowercaseNoVerify.put("dictionaryField", "f1");
        configNoLowercaseNoVerify.put("reverseCompoundTriggerWords", Arrays.asList("für", "aus"));
        final Map<String, Object> decompoundConfNoVerify = new HashMap<>();
        decompoundConfNoVerify.put("verifyCollation", false);
        configNoLowercaseNoVerify.put("decompound", decompoundConfNoVerify);
        configNoLowercaseNoVerify.put("lowerCaseInput", false);
        withRewriter(h.getCore(), "word_break_no_lc_no_verify_collation", WordBreakCompoundRewriterFactory.class,
                configNoLowercaseNoVerify);

        addDocs();
    }

    private static void addDocs() {

        assertU(adoc("id", "1",
                "f1", "herren"));
        assertU(adoc("id", "2",
                "f1", "herren schöne jacke"));
        assertU(adoc("id", "3",
                "f1", "damen"));
        assertU(adoc("id", "4",
                "f1", "jacke",
                "f2", "kinder"));
        assertU(adoc("id", "5",
                "f1", "damenjacke lila"));
        assertU(adoc("id", "6",
                "f1", "kinder"));
        assertU(adoc("id", "7",
                "f1", "bild buch"));
        assertU(commit());
        
    }

    @Test
    public void testWordBreakDecompounding() {
        String q = "herrenjacke";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                DisMaxParams.MM, "100%",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, "common_rules_before_wordbreak,word_break"
        );

        assertQ("Misssing decompound",
                req,
                "//result[@name='response' and @numFound='1']",
                "//doc/str[@name='id'][contains(.,'2')]"
        );


        req.close();
    }

    @Test
    public void testWordBreakDecompoundingGerman() {
        String q = "bilderbuch";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                DisMaxParams.MM, "100%",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, "common_rules_before_wordbreak,word_break_de"

        );

        assertQ("Misssing decompound",
                req,
                "//result[@name='response' and @numFound='1']",
                "//doc/str[@name='id'][contains(.,'7')]"
        );


        req.close();
    }

    @Test
    public void testThatLowerCaseTrueIsApplied() {
        String q = "Herrenjacke";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                DisMaxParams.MM, "100%",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, "common_rules_before_wordbreak,word_break"
        );

        assertQ("Misssing decompound",
                req,
                "//result[@name='response' and @numFound='1']",
                "//doc/str[@name='id'][contains(.,'2')]"
        );


        req.close();
    }

    @Test
    public void testThatLowerCaseFalseConfigIsApplied() {
        String q = "Herrenjacke";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                DisMaxParams.MM, "100%",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, "common_rules_before_wordbreak,word_break_no_lc_no_verify_collation"
        );

        assertQ("Misssing decompound",
                req,
                "//result[@name='response' and @numFound='0']"
        );


        req.close();
    }

    @Test
    public void testVerifyCollationInWordBreakDecompounding() {
        String q = "kinderjacke";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "100%",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, "common_rules_before_wordbreak,word_break"
        );

        assertQ("Decompound collation not verified",
                req,
                "//result[@name='response' and @numFound='0']"
        );

        req.close();
    }

    @Test
    public void testWordbreakDecompoundingValidatesAgainstConfiguredField() {
        String q = "kinderjacke";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "100%",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, "common_rules_before_wordbreak,word_break"
        );

        assertQ("Decompounding found unverified term",
                req,
                "//result[@name='response' and @numFound='0']"
        );

        req.close();
    }

    @Test
    public void testCompounding() {
        String q = "lila damen jacke";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "100%",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, "common_rules_before_wordbreak,word_break"
        );

        assertQ("Compounding failed",
                req,
                "//result[@name='response' and @numFound='1']/doc[1]/str[@name='id'][text()='5']"
        );

        req.close();
    }

    @Test
    public void testCompoundingTriggerReverseCompound() {
        String q = "jacke für damen lila";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "100%",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, "common_rules_before_wordbreak,word_break"
        );

        assertQ("Compounding with reverse trigger failed",
                req,
                "//result[@name='response' and @numFound='1']/doc[1]/str[@name='id'][text()='5']"
        );

        req.close();
    }

    @Test
    public void testDoNotVerifyCollationInWordBreakDecompounding() {
        String q = "kinderjacke";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                DisMaxParams.MM, "100%",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, "common_rules_before_wordbreak,word_break_no_lc_no_verify_collation"
        );

        assertQ("Decompound collation verified when it should not",
                req,
                "//result[@name='response' and @numFound='1']",
                "//doc/str[@name='id'][contains(.,'4')]"
        );

        req.close();
    }



}