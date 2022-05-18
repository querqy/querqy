package querqy.solr;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class QuerqyDismaxQParserPluginWithStopwordsTest extends SolrTestCaseJ4 {

    public void index() {
        assertU(adoc("id", "1", "f1", "a"));
        assertU(adoc("id", "2", "f1", "a"));
        assertU(adoc("id", "3", "f1", "a"));
        assertU(adoc("id", "4", "f1", "stopA"));

        assertU(commit());
    }

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig.xml", "schema-stopwords.xml");
        withCommonRulesRewriter(h.getCore(), "common_rules_empty", "configs/commonrules/rules-empty.txt");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearIndex();
        index();
    }

    @Test
    public void testQueryWithStopwords() {
        // Simple test make sure data loaded
        String q = "a";
        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "id f1",
                QueryParsing.OP, "OR",
                DisMaxParams.TIE, "0.1",
                "defType", "edismax",
                PARAM_REWRITERS, "common_rules_empty",
                CommonParams.DEBUG, "true",
                "indent", "true"
        );
        assertQ("Expected 3 results",
                req,
                "//result[@name='response'][@numFound='3']");

        req.close();

        q = "a";
        req = req("q", q,
                DisMaxParams.QF, "id f1",
                QueryParsing.OP, "OR",
                DisMaxParams.TIE, "0.1",
                "defType", "querqy",
                PARAM_REWRITERS, "common_rules_empty",
                CommonParams.DEBUG, "true",
                "indent", "true"
        );
        assertQ("Expected 3 results",
                req,
                "//result[@name='response'][@numFound='3']");

        req.close();


        // Check that no documents are found for only stop word
        q = "stopA";

        req = req("q", q,
                DisMaxParams.QF, "id f1",
                QueryParsing.OP, "OR",
                DisMaxParams.TIE, "0.1",
                "defType", "edismax",
                PARAM_REWRITERS, "common_rules_empty",
                CommonParams.DEBUG, "true",
                "indent", "true"
        );
        assertQ("No results expected",
                req,
                "//result[@name='response'][@numFound='0']");

        req.close();

        q = "stopA";

        req = req("q", q,
                DisMaxParams.QF, "id f1",
                QueryParsing.OP, "OR",
                DisMaxParams.TIE, "0.1",
                "defType", "querqy",
                PARAM_REWRITERS, "common_rules_empty",
                CommonParams.DEBUG, "true",
                "indent", "true"
        );
        assertQ("No results expected",
                req,
                "//result[@name='response'][@numFound='0']");

        req.close();

        // Test mix stop words and regular work without mm
        q = "stopA a";

        req = req("q", q,
                DisMaxParams.QF, "id f1",
                QueryParsing.OP, "OR",
                DisMaxParams.TIE, "0.1",
                "defType", "edismax",
                PARAM_REWRITERS, "common_rules_empty",
                CommonParams.DEBUG, "true",
                "indent", "true"
        );
        assertQ("Expected 3 results",
                req,
                "//result[@name='response'][@numFound='3']");

        req.close();

        q = "stopA a";

        req = req("q", q,
                DisMaxParams.QF, "id f1",
                QueryParsing.OP, "OR",
                DisMaxParams.TIE, "0.1",
                "defType", "querqy",
                PARAM_REWRITERS, "common_rules_empty",
                CommonParams.DEBUG, "true",
                "indent", "true"
        );
        assertQ("Expected 3 results",
                req,
                "//result[@name='response'][@numFound='3']");

        req.close();

        // Test mix stop words and regular work with mm

        q = "stopA a";

        req = req("q", q,
                DisMaxParams.QF, "id f1",
                QueryParsing.OP, "OR",
                DisMaxParams.MM, "2",
                "defType", "edismax",
                PARAM_REWRITERS, "common_rules_empty",
                CommonParams.DEBUG, "true",
                "indent", "true"
        );
        assertQ("Expected 3 results",
                req,
                "//result[@name='response'][@numFound='3']");

        req.close();

        q = "stopA a";

        req = req("q", q,
                DisMaxParams.QF, "id f1",
                QueryParsing.OP, "OR",
                DisMaxParams.MM, "2",
                "defType", "querqy",
                PARAM_REWRITERS, "common_rules_empty",
                CommonParams.DEBUG, "true",
                "indent", "true"
        );
        assertQ("Expected 3 results",
                req,
                "//result[@name='response'][@numFound='3']");

        req.close();
    }

}
