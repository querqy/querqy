package querqy.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.parser.WhiteSpaceQuerqyParser;

/**
 * Created by rene on 04/05/2017.
 */
@SolrTestCaseJ4.SuppressSSL
public class QuerqyDismaxQParserPluginConfigTest extends SolrTestCaseJ4 {

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-QuerqyDismaxQParserPluginConfigTest.xml", "schema.xml");
    }

    @Test
    public void testThatFactoryConfigIsAvailable() throws Exception {
        String q = "*:*";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy1",
                "debugQuery", "true"
        );

        assertQ("Config for querqy1 fails",
                req,
                "//str[@name='querqy.parser'][text() = '" + DummyQuerqyParser.class.getName() + "']"
        );

        req.close();
    }

    @Test
    public void testThatParserClassConfigIsAvailable() throws Exception {
        String q = "*:*";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy2",
                "debugQuery", "true"
        );

        assertQ("Config for querqy2 fails",
                req,
                "//str[@name='querqy.parser'][text() = '" + DummyQuerqyParser.class.getName() + "']"
        );

        req.close();
    }

    @Test
    public void testThatParserConfigIsAvailable() throws Exception {
        String q = "*:*";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy3",
                "debugQuery", "true"
        );

        assertQ("Config for querqy3 fails",
                req,
                "//str[@name='querqy.parser'][text() = '" + DummyQuerqyParser.class.getName() + "']"
        );

        req.close();
    }

    @Test
    public void testThatWhiteSpaceQuerqyParserIsTheDefault() throws Exception {
        String q = "*:*";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy4",
                "debugQuery", "true"
        );

        assertQ("Config for querqy4 fails",
                req,
                "//str[@name='querqy.parser'][text() = '" + WhiteSpaceQuerqyParser.class.getName() + "']"
        );

        req.close();
    }
}
