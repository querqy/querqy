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
                "f1", "herren"));
        assertU(adoc("id", "2",
                "f1", "herren schöne jacke"));
        assertU(adoc("id", "3",
                "f1", "damen"));
        assertU(adoc("id", "4",
                "f1", "jacke",
                "f2", "kinder"));
        assertU(adoc("id", "5",
                "f1", "damenjacke lila"));
        assertU(adoc("id", "6",
                "f1", "kinder"));
        assertU(commit());
        
    }

    @Test
    public void testWordBreakDecompounding() {
        String q = "herrenjacke";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                DisMaxParams.MM, "100%",
                "defType", "querqy",
                "debugQuery", "on"
        );

        assertQ("Misssing decompound",
                req,
                "//result[@name='response' and @numFound='1']",
                "//doc/str[@name='id'][contains(.,'2')]"
        );


        req.close();
    }

    @Test
    public void testVerifyCollationInWordBreakDecompounding() {
        String q = "kinderjacke";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "100%",
                "defType", "querqy",
                "debugQuery", "on"
        );

        assertQ("Decompound collation not verified",
                req,
                "//result[@name='response' and @numFound='0']"
        );

        req.close();
    }

    @Test
    public void testWordbreakDecompoundingValidatesAgainstConfiguredField() {
        String q = "kinderjacke";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "100%",
                "defType", "querqy",
                "debugQuery", "on"
        );

        assertQ("Decompounding found unverified term",
                req,
                "//result[@name='response' and @numFound='0']"
        );

        req.close();
    }

    @Test
    public void testCompounding() {
        String q = "lila damen jacke";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "100%",
                "defType", "querqy",
                "debugQuery", "on"
        );

        assertQ("Compounding failed",
                req,
                "//result[@name='response' and @numFound='1']/doc[1]/str[@name='id'][text()='5']"
        );

        req.close();
    }

    @Test
    public void testCompoundingTriggerReverseCompound() {
        String q = "jacke für damen lila";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "100%",
                "defType", "querqy",
                "debugQuery", "on"
        );

        assertQ("Compounding with reverse trigger failed",
                req,
                "//result[@name='response' and @numFound='1']/doc[1]/str[@name='id'][text()='5']"
        );

        req.close();
    }

    @Test
    public void testDoNotVerifyCollationInWordBreakDecompounding() {
        String q = "kinderjacke";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2 f3",
                DisMaxParams.MM, "100%",
                "defType", "querqyNoCollation",
                "debugQuery", "on"
        );

        assertQ("Decompound collation verified when it should not",
                req,
                "//result[@name='response' and @numFound='1']",
                "//doc/str[@name='id'][contains(.,'4')]"
        );

        req.close();
    }



}