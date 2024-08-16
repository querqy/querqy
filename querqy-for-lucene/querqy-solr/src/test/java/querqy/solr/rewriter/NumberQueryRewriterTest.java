package querqy.solr.rewriter;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.hamcrest.collection.IsMapContaining;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;
import static querqy.solr.StandaloneSolrTestSupport.withRewriter;

@SolrTestCaseJ4.SuppressSSL
public class NumberQueryRewriterTest extends SolrTestCaseJ4 {

    private final static String REWRITERS = "common_rules_before_shingles,shingles,common_rules_after_shingles";

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig.xml", "schema.xml");
        withCommonRulesRewriter(h.getCore(), "common_rules_before_shingles",
                "configs/commonrules/rules-before-shingles.txt");
        final Map<String, Object> config = new HashMap<>();
        config.put("acceptGeneratedTerms", false);
        withRewriter(h.getCore(), "shingles", NumberQueryRewriterFactory.class, config);
        withCommonRulesRewriter(h.getCore(), "common_rules_after_shingles",
                "configs/commonrules/rules-shingles.txt");
    }

    @Test
    public void testOnTwoNumberTerms() {
        String q = "01 23 c";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Missing shingles",
                req,
                "//str[@name='parsedquery'][contains(.,'0123')]",
                "//str[@name='parsedquery'][not(contains(.,'23c'))]"

        );

        req.close();
    }

    @Test
    public void testOnShortNumberTerms() {
        String q = "1 23 c";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Missing shingles",
                req,
                "//str[@name='parsedquery'][contains(.,'123')]",
                "//str[@name='parsedquery'][not(contains(.,'23c'))]"

        );

        req.close();
    }

    @Test
    public void testOnTooShortNumberTerms() {
        String q = "1 2 c";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Missing shingles",
                req,
                "//str[@name='parsedquery'][not(contains(.,'12'))]",
                "//str[@name='parsedquery'][not(contains(.,'2c'))]"

        );

        req.close();
    }

    @Test
    public void testOnMultipleNumberTerms() {
        String q = "01 23 c 45 67";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Missing shingles",
                req,
                "//str[@name='parsedquery'][contains(.,'0123')]",
                "//str[@name='parsedquery'][contains(.,'4567')]"

        );

        req.close();
    }

    @Test
    public void testOnNumberTermsOnly() {
        String q = "01 23 45 67";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Wrong shingles",
                req,
                "//str[@name='parsedquery'][contains(.,'01234567')]",
                "//str[@name='parsedquery'][not(contains(.,':4567'))]"

        );

        req.close();
    }
    @Test
    public void testNumberAndSpecialCharCombination() {
        String q = "01.23 45 67";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Wrong shingles",
                req,
                "//str[@name='parsedquery'][contains(.,'4567')]",
                "//str[@name='parsedquery'][not(contains(.,'0123'))]"

        );

        req.close();
    }

    @Test
    public void testThatNumberShinglesAreNotCreatedOnWordOrGeneratedTerms() {
        String q = "t1 t2";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, REWRITERS
        );

        assertQ("Problem with shingles on generated terms",
                req,
                "//str[@name='parsedquery'][not(contains(.,'t1t2'))]",
                "//str[@name='parsedquery'][not(contains(.,'s1t2'))]"

        );

        req.close();
    }

    @Test
    public void testConfigRequestAcceptGeneratedTerms() {
        final Map<String, Object> config = new NumberQueryRewriterFactory.NumberQueryConfigRequestBuilder()
                .acceptGeneratedTerms(true).buildConfig();

        org.hamcrest.MatcherAssert.assertThat(config, IsMapContaining.hasEntry(
                NumberQueryRewriterFactory.CONF_ACCEPT_GENERATED_TERMS, Boolean.TRUE));

        final NumberQueryRewriterFactory factory = new NumberQueryRewriterFactory("id");

        final List<String> errors = factory.validateConfiguration(config);
        assertTrue(errors == null || errors.isEmpty());

        // TODO: read the acceptGeneratedTerms property once it becomes accessible

        // make sure that no exception  is thrown
        factory.configure(config);

    }

    @Test
    public void testConfigRequestDoNotAcceptGeneratedTerms() {
        final Map<String, Object> config = new NumberQueryRewriterFactory.NumberQueryConfigRequestBuilder()
                .acceptGeneratedTerms(false).buildConfig();

        org.hamcrest.MatcherAssert.assertThat(config, IsMapContaining.hasEntry(
                ShingleRewriterFactory.CONF_ACCEPT_GENERATED_TERMS, Boolean.FALSE));

        final NumberQueryRewriterFactory factory = new NumberQueryRewriterFactory("id");

        final List<String> errors = factory.validateConfiguration(config);
        assertTrue(errors == null || errors.isEmpty());

        // TODO: read the acceptGeneratedTerms property once it becomes accessible

        // make sure that no exception  is thrown
        factory.configure(config);

    }

    @Test
    public void testConfigRequestDefaultAcceptGeneratedTerms() {
        final Map<String, Object> config = new NumberQueryRewriterFactory.NumberQueryConfigRequestBuilder().buildConfig();

        assertTrue(config.isEmpty());

        final NumberQueryRewriterFactory factory = new NumberQueryRewriterFactory("id");

        final List<String> errors = factory.validateConfiguration(config);
        assertTrue(errors == null || errors.isEmpty());

        // TODO: read the acceptGeneratedTerms property once it becomes accessible

        // make sure that no exception  is thrown
        factory.configure(config);

    }

}
