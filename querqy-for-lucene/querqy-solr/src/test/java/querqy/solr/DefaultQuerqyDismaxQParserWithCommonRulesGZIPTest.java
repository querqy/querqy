package querqy.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class DefaultQuerqyDismaxQParserWithCommonRulesGZIPTest extends SolrTestCaseJ4 {

    public void index() {
        assertU(adoc("id", "1", "f1", "a", "f2", "c"));
        assertU(commit());
    }

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-commonrules-gz.xml", "schema.xml");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearIndex();
        index();
    }

    @Test
    public void testSolrFilterQuery() {

        String q = "a k";

        SolrQueryRequest req = req("q", q,
              DisMaxParams.QF, "f1 f2 f3",
              DisMaxParams.MM, "1",
              QueryParsing.OP, "OR",
              "defType", "querqy"
              );

        assertQ("Solr filter query fails",
              req,
              "//result[@name='response' and @numFound='1']"

        );

        req.close();
    }

}
