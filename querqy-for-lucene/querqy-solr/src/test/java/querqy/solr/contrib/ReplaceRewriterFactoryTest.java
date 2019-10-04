package querqy.solr.contrib;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class ReplaceRewriterFactoryTest extends SolrTestCaseJ4 {

    @Test
    public void testDefaults() throws Exception {
        initCore("contrib/solrconfig-replace-defaults.xml", "schema.xml");

        String q = "a b d";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                "defType", "querqy",
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
        initCore("contrib/solrconfig-replace-synonyms.xml", "schema.xml");

        String q = "a b b c d";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                "defType", "querqy",
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
