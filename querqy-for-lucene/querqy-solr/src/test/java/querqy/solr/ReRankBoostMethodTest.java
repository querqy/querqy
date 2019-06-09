package querqy.solr;

import static querqy.solr.QuerqyDismaxParams.QBOOST_METHOD;
import static querqy.solr.QuerqyDismaxParams.QBOOST_METHOD_RERANK;
import static querqy.solr.QuerqyDismaxParams.QBOOST_RERANK_NUMDOCS;
import static querqy.solr.QuerqyDismaxParams.QBOOST_SIMILARITY_SCORE;
import static querqy.solr.QuerqyDismaxParams.SIMILARITY_SCORE_OFF;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by rene on 05/09/2016.
 */
@SolrTestCaseJ4.SuppressSSL
public class ReRankBoostMethodTest extends SolrTestCaseJ4 {

    public void index() {

        assertU(adoc("id", "1", "f1", "qup"));
        assertU(adoc("id", "2", "f1", "qup other", "f2", "u100"));
        assertU(adoc("id", "3", "f1", "qdown1 d2"));
        assertU(adoc("id", "4", "f1", "qdown2 d1"));
        assertU(commit());
    }

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-commonrules.xml", "schema.xml");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearIndex();
        index();
    }

    @Test
    public void testThatReRankBringsBoostedDocToTop() {

        String q = "qup";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR",
                QBOOST_METHOD, QBOOST_METHOD_RERANK,
                "defType", "querqy",
                "debugQuery", "true"

        );

        // doc 2 gets a better score by boosting f2:u100
        assertQ("Result is not re-ranked",
                req,
                "//result[@name='response'][@numFound='2']",
                "//doc[1]/str[@name='id'][text()='2']",
                "//doc[2]/str[@name='id'][text()='1']",
                "//lst[@name='explain']/str[contains(.,'QuerqyReRankQuery')]"
        );
        req.close();

        // counter-check using edismax
        SolrQueryRequest req2 = req("q", q,
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR",
                "defType", "edismax",
                "debugQuery", "true"

        );

        // doc 2 gets a worse score due to doc length normalisation
        assertQ("Edismax counter-check fails",
                req2,
                "//result[@name='response'][@numFound='2']",
                "//doc[1]/str[@name='id'][text()='1']",
                "//doc[2]/str[@name='id'][text()='2']",
                "//lst[@name='explain']/str[not(contains(.,'QuerqyReRankQuery'))]"
        );
        req2.close();

    }

    @Test
    public void testThatOnlyTopNDocsAreReRanked() throws Exception {


        String q = "qup";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR",
                QBOOST_METHOD, QBOOST_METHOD_RERANK,
                QBOOST_RERANK_NUMDOCS, "1", // try to re-rank only one doc --> will not change order
                "defType", "querqy",
                "debugQuery", "true"

        );

        assertQ("bm.rerank.numDocs doesn't work",
                req,
                "//result[@name='response'][@numFound='2']",
                "//doc[1]/str[@name='id'][text()='1']",
                "//doc[2]/str[@name='id'][text()='2']",
                "//lst[@name='explain']/str[contains(.,'QuerqyReRankQuery')]"
        );
        req.close();


        // counter-check: re-rank both docs
        SolrQueryRequest req2 = req("q", q,
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR",
                QBOOST_METHOD, QBOOST_METHOD_RERANK,
                QBOOST_RERANK_NUMDOCS, "2", // re-rank both docs
                "defType", "querqy",
                "debugQuery", "true"

        );

        assertQ("bm.rerank.numDocs doesn't work",
                req2,
                "//result[@name='response'][@numFound='2']",
                "//doc[1]/str[@name='id'][text()='2']",
                "//doc[2]/str[@name='id'][text()='1']",
                "//lst[@name='explain']/str[contains(.,'QuerqyReRankQuery')]"
        );
        req2.close();
    }


    @Test
    public void testThatReRankWorksForNegativeBoostWithSimilarityOff() {
        String q = "qdown1 qdown2";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                QueryParsing.OP, "OR",
                QBOOST_METHOD, QBOOST_METHOD_RERANK,
                QBOOST_SIMILARITY_SCORE, SIMILARITY_SCORE_OFF,
                "defType", "querqy",
                "debugQuery", "true"

        );

        assertQ("Rerank does not work with negative boost'",
                req,
                "//result[@name='response'][@numFound='2']",
                // the parsed query must contain not the boost terms:
                "//str[@name='parsedquery'][not(contains(.,'f1:d1'))]",
                "//str[@name='parsedquery'][not(contains(.,'f1:d2'))]",
                // debug output must contain 'QuerqyReRankQuery'
                "//lst[@name='explain']/str[contains(.,'QuerqyReRankQuery')]",
                "//doc[1]/str[@name='id'][contains(.,'4')]",
                "//doc[2]/str[@name='id'][contains(.,'3')]",
                "//str[@name='4'][contains(.,'0.2 = AdditiveBoostFunction(-0.2,query(+f1:d2,def=0.0)=0.0)')]",
                "//str[@name='4'][contains(.,'0.05 = AdditiveBoostFunction(-0.1,query(+f1:d1,def=0.0)=1.0)')]",
                "//str[@name='3'][contains(.,'0.1 = AdditiveBoostFunction(-0.2,query(+f1:d2,def=0.0)=1.0)')]",
                "//str[@name='3'][contains(.,'0.1 = AdditiveBoostFunction(-0.1,query(+f1:d1,def=0.0)=0.0)')]"

        );
        req.close();

    }

}
