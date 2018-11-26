package querqy.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;

public class WordBreakCompoundRewriterTest extends SolrTestCaseJ4 {

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-wordbreak.xml", "schema.xml");
        addDocs();
    }

    private static void addDocs() {
        assertU(adoc("id", "1",
                "f1", "herrenjacke"));
        assertU(adoc("id", "2",
                "f1", "herren"));
        assertU(adoc("id", "3",
                "f1", "damen"));
        assertU(adoc("id", "3",
                "f1", "jacke",
                "f2", "kinder"));
        assertU(commit());
    }

    @Test
    public void testWordbreakDecompounding() {
        String q = "schöne herrenjacke";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                "defType", "querqy",
                "debugQuery", "on"
        );

        // todo: test for existence of (herrenjacke OR herren) (herrenjacke OR jacke)

        // ...

        req.close();
    }
}