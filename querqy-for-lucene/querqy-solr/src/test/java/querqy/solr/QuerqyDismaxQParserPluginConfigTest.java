package querqy.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
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
    public void testThatFactoryConfigIsAvailable() {
        String q = "a";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy1",
                "debugQuery", "true"
        );

        assertQ("Config for querqy1 fails",
                req,
                "//lst[@name='querqy']/str[@name='parser'][text() = '" + DummyQuerqyParser.class.getName() + "']"
        );

        req.close();
    }

    @Test
    public void testThatParserClassConfigIsAvailable() {
        String q = "a";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy2",
                "debugQuery", "true"
        );

        assertQ("Config for querqy2 fails",
                req,
                "//lst[@name='querqy']/str[@name='parser'][text() = '" + DummyQuerqyParser.class.getName() + "']"
        );

        req.close();
    }

    @Test
    public void testThatParserConfigIsAvailable() {
        String q = "a";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy3",
                "debugQuery", "true"
        );

        assertQ("Config for querqy3 fails",
                req,
                "//lst[@name='querqy']/str[@name='parser'][text() = '" + DummyQuerqyParser.class.getName() + "']"
        );

        req.close();
    }

    @Test
    public void testThatWhiteSpaceQuerqyParserIsTheDefault() {
        String q = "a";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy4",
                "debugQuery", "true"
        );

        assertQ("Config for querqy4 fails",
                req,
                "//lst[@name='querqy']/str[@name='parser'][text() = '" + WhiteSpaceQuerqyParser.class.getName() + "']"
        );

        req.close();
    }
}
