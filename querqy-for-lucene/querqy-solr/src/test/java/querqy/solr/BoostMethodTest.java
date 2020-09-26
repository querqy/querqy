package querqy.solr;

import static querqy.solr.QuerqyDismaxParams.QBOOST_METHOD;
import static querqy.solr.QuerqyDismaxParams.QBOOST_METHOD_OPT;
import static querqy.solr.QuerqyDismaxParams.QBOOST_METHOD_RERANK;
import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by rene on 01/09/2016.
 */
@SolrTestCaseJ4.SuppressSSL
public class BoostMethodTest extends SolrTestCaseJ4 {

    public void index() {

        assertU(adoc("id", "1", "f1", "qup"));
        assertU(adoc("id", "2", "f1", "qup other", "f2", "u100"));
        assertU(commit());
    }

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig.xml", "schema.xml");
        withCommonRulesRewriter(h.getCore(), "common_rules", "configs/commonrules/rules.txt");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearIndex();
        index();
    }

    @Test
    public void testThatTheDefaultBoostMethodIsOpt() {
        String q = "qup";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("Default boost method is not 'opt'",
                req,
                "//result[@name='response'][@numFound='2']",
                // the parsed query must contain the boost terms:
                "//str[@name='parsedquery'][contains(.,'f1:u100')]",
                "//str[@name='parsedquery'][contains(.,'f2:u100')]");
        req.close();

    }

    @Test
    public void testThatOptCanBePassedAsBoostMethodParam() {
        String q = "qup";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR",
                QBOOST_METHOD, QBOOST_METHOD_OPT,
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules"

        );

        assertQ("Default boost method is not 'opt'",
                req,
                "//result[@name='response'][@numFound='2']",
                // the parsed query must contain the boost terms:
                "//str[@name='parsedquery'][contains(.,'f1:u100')]",
                "//str[@name='parsedquery'][contains(.,'f2:u100')]");
        req.close();

    }

    @Test
    public void testThatReRankMethodCanBeActivated() {
        String q = "qup";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR",
                QBOOST_METHOD, QBOOST_METHOD_RERANK,
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules"

        );

        assertQ("Method is not 'rerank'",
                req,
                "//result[@name='response'][@numFound='2']",
                // the parsed query must contain not the boost terms:
                "//str[@name='parsedquery'][not(contains(.,'f1:u100'))]",
                "//str[@name='parsedquery'][not(contains(.,'f2:u100'))]",
                // debug output must contain 'QuerqyReRankQuery'
                "//lst[@name='explain']/str[contains(.,'QuerqyReRankQuery')]"
        );
        req.close();

    }


}
