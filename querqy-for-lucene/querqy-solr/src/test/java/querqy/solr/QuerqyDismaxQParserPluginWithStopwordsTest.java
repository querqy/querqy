package querqy.solr;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class QuerqyDismaxQParserPluginWithStopwordsTest extends SolrTestCaseJ4 {

    public void index() {

        assertU(adoc("id", "1", "f1_stopwords", "a", "f2_stopwords", "c"));

        assertU(adoc("id", "2", "f1_stopwords", "a", "f2_stopwords", "b"));
        
        assertU(adoc("id", "3", "f1_stopwords", "a", "f3", "c"));
        
        assertU(adoc("id", "4", "f3", "stopA"));

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
    public void testThatStopwordOnlyQueryReturnsNoResult() {

        String q = "stopA";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1_stopwords f2_stopwords",
                QueryParsing.OP, "OR",
                DisMaxParams.TIE, "0.1",
                "defType", "querqy",
                PARAM_REWRITERS, "common_rules_empty",
                "debugQuery", "true"
              );
        assertQ("No results expected",
              req,
              "//result[@name='response'][@numFound='0']");

        req.close();
     }

}
