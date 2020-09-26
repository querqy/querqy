package querqy.solr.rewriter.commonrules;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.solr.BoostMethodTest;

import java.io.IOException;

/**
 * Created by rene on 05/05/2017.
 */
@SolrTestCaseJ4.SuppressSSL
public class CommonRulesConfigTest extends SolrTestCaseJ4 {

    @BeforeClass
    public static void beforeTests() throws Exception {

        initCore("solrconfig.xml", "schema.xml");

        try {
            final CommonRulesConfigRequestBuilder builder = new CommonRulesConfigRequestBuilder()
                    .rules(BoostMethodTest.class.getClassLoader()
                            .getResourceAsStream("configs/commonrules/rules.txt"));
            builder.ignoreCase(true);
            withCommonRulesRewriter(h.getCore(), "common_rules2", builder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            final CommonRulesConfigRequestBuilder builder = new CommonRulesConfigRequestBuilder()
                    .rules(BoostMethodTest.class.getClassLoader()
                            .getResourceAsStream("configs/commonrules/rules.txt"));
            builder.ignoreCase(false);
            withCommonRulesRewriter(h.getCore(), "common_rules3", builder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            final CommonRulesConfigRequestBuilder builder = new CommonRulesConfigRequestBuilder()
                    .rules(BoostMethodTest.class.getClassLoader()
                            .getResourceAsStream("configs/commonrules/rules.txt"));
            withCommonRulesRewriter(h.getCore(), "common_rules4", builder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testIgnoreCaseFalse() {

        String q = "M";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules3"
        );

        assertQ("Config for ignoreCase=false fails",
                req,
                "//str[@name='parsedquery'][not(contains(.,'f1:d'))]"
        );

        req.close();
    }

    @Test
    public void testIgnoreCaseTrue() {

        String q = "M";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules2"
        );

        assertQ("Config for ignoreCase=true fails",
                req,
                "//str[@name='parsedquery'][contains(.,'f1:d')]"
        );

        req.close();
    }

    @Test
    public void testIgnoreCaseIsTrueByDefault() {

        String q = "M";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules4"
        );

        assertQ("Default value for ignoreCase fails",
                req,
                "//str[@name='parsedquery'][contains(.,'f1:d')]"
        );

        req.close();
    }

    @Test
    public void testThatWhiteSpaceQuerqyParserIsUsedForRighHandSideByDefault() throws Exception {

        String q = "qneg2";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules4"
        );

        assertQ("Default RHS QuerqyParser fails",
                req,
                "//arr[@name='parsed_filter_queries']/str[contains(.,'f1:qneg') and contains(.,'-f1:k')]"
        );

        req.close();
    }

}
