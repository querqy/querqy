package querqy.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.parser.WhiteSpaceQuerqyParser;

/**
 * Created by rene on 05/05/2017.
 */
@SolrTestCaseJ4.SuppressSSL
public class CommonRulesConfigTest extends SolrTestCaseJ4 {

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-QuerqyDismaxQParserPluginConfigTest.xml", "schema.xml");
    }

    @Test
    public void testIgnoreCaseFalse() throws Exception {

        String q = "M";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy3",
                "debugQuery", "true"
        );

        assertQ("Config for ignoreCase=false fails",
                req,
                "//str[@name='parsedquery'][not(contains(.,'f1:d'))]"
        );

        req.close();
    }

    @Test
    public void testIgnoreCaseTrue() throws Exception {

        String q = "M";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy2",
                "debugQuery", "true"
        );

        assertQ("Config for ignoreCase=true fails",
                req,
                "//str[@name='parsedquery'][contains(.,'f1:d')]"
        );

        req.close();
    }

    @Test
    public void testIgnoreCaseIsTrueByDefault() throws Exception {

        String q = "M";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy4",
                "debugQuery", "true"
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
                "defType", "querqy4",
                "debugQuery", "true"
        );

        assertQ("Default RHS QuerqyParser fails",
                req,
                "//arr[@name='parsed_filter_queries']/str[contains(.,'f1:qneg') and contains(.,'-f1:k')]"
        );

        req.close();
    }

}
