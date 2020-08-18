package querqy.solr;

import static querqy.solr.QuerqyDismaxParams.FBM;
import static querqy.solr.QuerqyDismaxParams.FBM_FIXED;
import static querqy.solr.QuerqyDismaxParams.FBM_PRMS;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class FieldBoostTest extends SolrTestCaseJ4 {

    public void index() throws Exception {

        assertU(adoc("id", "1", "f1", "m", "f2", "o x", "f3", "h"));

        assertU(adoc("id", "2", "f1", "n", "f2", "p x"));

        assertU(adoc("id", "3", "f1", "n", "f2", "q x"));

        assertU(adoc("id", "4", "f1", "x", "f2", "r", "f3", "h"));

        assertU(commit());
    }

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-dfc.xml", "schema.xml");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearIndex();
        index();
    }

    @Test
    public void testFixedModel() {

        String q = "x";

        SolrQueryRequest req = req("q", q, DisMaxParams.QF, "f1 f2",
                FBM, FBM_FIXED,
                "defType", "querqy");

        assertQ("FieldBoost=FIXED should sort doc with greatest length-normalized tf to the first position",
                req, "//result/doc[1]/str[@name='id'][text()='4']");
    }

    
    @Test
    public void testThatFixedModelIsDefaultModel() {

        String q = "x";

        SolrQueryRequest req = req("q", q, DisMaxParams.QF, "f1 f2",
                "defType", "querqy");

        assertQ("FieldBoost=FIXED should sort doc with greatest length-normalized tf to the first position",
                req, "//result/doc[1]/str[@name='id'][text()='4']");
    }
    
    @Test
    public void testThatPRMSScoresLeastFrequentFieldLowest() {

        String q = "x";

        SolrQueryRequest req = req("q", q, DisMaxParams.QF, "f1 f2",
                FBM, FBM_PRMS,
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("FieldBoost=PRMS should sort doc with greatest length-normalized tf to the last position",
                req, "//result/doc[4]/str[@name='id'][text()='4']");
    }
    
    @Test
    public void testThatPRMSIsAppliedToMultiTermQuery() {

        String q = "x h";

        SolrQueryRequest req = req("q", q, DisMaxParams.QF, "f1 f2 f3",
                FBM, FBM_PRMS,
                DisMaxParams.MM, "100%",
                "defType", "querqy");

        assertQ("FieldBoost=PRMS should sort doc with greatest length-normalized tf to the last position",
                req, "//result/doc[2]/str[@name='id'][text()='4']");
    }
    
    @Test
    public void testThatPRMSCanHandleTermNotInIndex() {

        String q = "y24";

        SolrQueryRequest req = req("q", q, DisMaxParams.QF, "f1 f2 f3",
                FBM, FBM_PRMS,
                DisMaxParams.MM, "100%",
                "defType", "querqy");

        assertQ("FieldBoost=PRMS failed to handle term that is not in index",
                req, "//result[@numFound='0']");
    }

}
