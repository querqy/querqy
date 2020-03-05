package querqy.solr.contrib;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class ReplaceRewriterFactoryTest extends SolrTestCaseJ4 {

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("contrib/solrconfig-replace.xml", "schema.xml");
    }

    @Test
    public void testMatchAllQuery() {
        String q = "*:*";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                "defType", "querqy_defaults",
                "debugQuery", "on"
        );

        assertQ("", req, "//result[@name='response' and @numFound='0']");
        req.close();
    }


    @Test
    public void testDefaults() throws Exception {
        String q = "a b d";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                "defType", "querqy_defaults",
                "debugQuery", "on"
        );

        assertQ("Missing shingles",
                req,
                "//str[@name='parsedquery'][contains(.,'e')]",
                "//str[@name='parsedquery'][contains(.,'f')]",
                "//str[@name='parsedquery'][contains(.,'g')]"
        );

        req.close();
    }

    @Test
    public void testSynonymsAfterReplacement() throws Exception {
        String q = "a b b c d";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                "defType", "querqy_synonyms",
                "debugQuery", "on"
        );

        assertQ("Missing shingles",
                req,
                "//str[@name='parsedquery'][contains(.,'e')]",
                "//str[@name='parsedquery'][contains(.,'f')]",
                "//str[@name='parsedquery'][contains(.,'g')]",
                "//str[@name='parsedquery'][contains(.,'h')]",
                "//str[@name='parsedquery'][contains(.,'i')]"
        );

        req.close();
    }


}
