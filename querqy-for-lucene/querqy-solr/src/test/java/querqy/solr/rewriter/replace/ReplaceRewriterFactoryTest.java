package querqy.solr.rewriter.replace;

import org.apache.commons.io.IOUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.lucene.GZIPAwareResourceLoader;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory;
import querqy.solr.StandaloneSolrTestSupport;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.RewriterConfigRequestBuilder.CONF_CLASS;
import static querqy.solr.RewriterConfigRequestBuilder.CONF_CONFIG;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;
import static querqy.solr.StandaloneSolrTestSupport.withRewriter;
import static querqy.solr.rewriter.replace.ReplaceRewriterFactory.*;

@SolrTestCaseJ4.SuppressSSL
public class ReplaceRewriterFactoryTest extends SolrTestCaseJ4 {

    private static final String FILE_NAME = "f1";
    private final ReplaceRewriterFactory factory = new ReplaceRewriterFactory("test");

    @BeforeClass
    public static void beforeTests() throws Exception {

        initCore("solrconfig.xml", "schema.xml");

        final Map<String, Object> config = new HashMap<>();
        config.put(CONF_RULES,
                StandaloneSolrTestSupport.resourceToString("configs/replace/replace-rules-defaults.txt"));
        withRewriter(h.getCore(), "replace_defaults", ReplaceRewriterFactory.class, config);

        final Map<String, Object> config2 = new HashMap<>();
        config2.put(CONF_RULES,
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

    @Test
    public void testThatDeprecatedConfigurationIsCorrectlyParsed() throws IOException {

        final GZIPAwareResourceLoader resourceLoader = mock(GZIPAwareResourceLoader.class);

        when(resourceLoader.openResource(FILE_NAME)).thenReturn(IOUtils.toInputStream("mobiles; ombile; mo bile => mobile\n" +
                "cheapest smartphones => cheap smartphone", UTF_8));

        NamedList<Object> configuration = new NamedList<>();
        configuration.add(CONF_CLASS, ReplaceRewriterFactory.class.getName());
        configuration.add(CONF_IGNORE_CASE, true);
        configuration.add(CONF_RHS_QUERY_PARSER, WhiteSpaceQuerqyParserFactory.class.getName());
        configuration.add(CONF_RULES, "f1");

        Map<String, Object> parsed = factory.parseConfigurationToRequestHandlerBody(configuration, resourceLoader);
        // no exceptions!
        factory.configure((Map<String, Object>) parsed.get(CONF_CONFIG));

        RewriterFactory rewriterFactory = factory.getRewriterFactory();

        Assertions.assertThat(rewriterFactory).isInstanceOf(querqy.rewrite.contrib.ReplaceRewriterFactory.class);
        Assertions.assertThat(factory.validateConfiguration((Map<String, Object>) parsed.get(CONF_CONFIG))).isNull();
    }
    
    @Test
    public void testThatNonUtf8EncodedRulesAreParsed() throws NoSuchFieldException, IllegalAccessException {
        resetCachedDefaultCharsetConstant();
        
        String previousFileEncodingSystemPropertyValue = System.getProperty("file.encoding");
        System.setProperty("file.encoding", "US-ASCII");
        
        try {
            String rules = "veľkostná; veľkostné => velkostna";
            Map<String, Object> params = Map.of(
                    CONF_RULES, rules,
                    CONF_INPUT_DELIMITER, ";"
            );
            Assertions.assertThat(factory.validateConfiguration(params)).isNull();
        } finally {
            resetCachedDefaultCharsetConstant();
            if (previousFileEncodingSystemPropertyValue == null) {
                System.clearProperty("file.encoding");
            } else {
                System.setProperty("file.encoding", previousFileEncodingSystemPropertyValue);
            }
        }
    }
    
    private static void resetCachedDefaultCharsetConstant() throws NoSuchFieldException, IllegalAccessException {
        Field charset = Charset.class.getDeclaredField("defaultCharset");
        charset.setAccessible(true);
        charset.set(null, null);
    }     
        
}
