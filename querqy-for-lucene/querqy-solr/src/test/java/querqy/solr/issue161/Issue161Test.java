package querqy.solr.issue161;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;
import static querqy.solr.StandaloneSolrTestSupport.withRewriter;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.solr.QuerqyDismaxParams;
import querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SolrTestCaseJ4.SuppressSSL
public class Issue161Test extends SolrTestCaseJ4 {

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-issue161.xml", "schema.xml");

        final Map<String, Object> config = new HashMap<>();
        config.put("dictionaryField", "f1");
        final Map<String, Object> decompoundConf = new HashMap<>();
        decompoundConf.put("verifyCollation", true);
        config.put("decompound", decompoundConf);
        config.put("lowerCaseInput", true);
        withRewriter(h.getCore(), "word_break", WordBreakCompoundRewriterFactory.class, config);

        withCommonRulesRewriter(h.getCore(), "common_rules", "issue161/rules.txt");

        addDocs();
    }

    private static void addDocs() {
        assertU(adoc("id", "1", "f1", "herren jacke"));
        assertU(commit());
    }

    @Test
    public void testThatTheLogPropertyIsReturned() {

        String q = "herrenjacke";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                DisMaxParams.MM, "100%",
                QuerqyDismaxParams.INFO_LOGGING, "on",
                "defType", "querqy",
                PARAM_REWRITERS, "word_break,common_rules",
                "debug", "true"
        );

        assertQ("Log property is missing",
                req,
                "count(//lst[@name='querqy.infoLog']/arr[@name='common_rules']/lst/arr[@name='APPLIED_RULES']) = 1"
        );

        req.close();
    }
}
