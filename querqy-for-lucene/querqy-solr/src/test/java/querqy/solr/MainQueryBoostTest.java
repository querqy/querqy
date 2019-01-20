package querqy.solr;

import static querqy.solr.QuerqyDismaxParams.QBOOST_NEG_WEIGHT;
import static querqy.solr.QuerqyDismaxParams.QBOOST_WEIGHT;
import static querqy.solr.QuerqyDismaxParams.SIMILARITY_SCORE_DFC;
import static querqy.solr.QuerqyDismaxParams.SIMILARITY_SCORE_OFF;
import static querqy.solr.QuerqyDismaxParams.USER_QUERY_BOOST;
import static querqy.solr.QuerqyDismaxParams.USER_QUERY_SIMILARITY_SCORE;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class MainQueryBoostTest extends SolrTestCaseJ4 {

    public void index() {

        assertU(adoc("id", "1", "f1", "qup"));
        assertU(adoc("id", "2", "f1", "qup other", "f2", "u100"));
        assertU(adoc("id", "3", "f1", "m", "f2", "d"));
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
    public void testIDFIsOnlyUsedInBoostQueryAndNotInUserQueryIfSimilarityIsOff() {
        String q = "qup";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1^10 f2^2",
                QueryParsing.OP, "OR",
                USER_QUERY_SIMILARITY_SCORE, SIMILARITY_SCORE_OFF,
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("uq.similarityScore=off failed",
                req,
                "//result[@name='response'][@numFound='2']",
                "//str[@name='1'][not(contains(.,'idf'))]", // no boost query for doc 1 -> no idf for user query
                "//str[@name='2'][contains(.,'idf')]"); // expecting a boost query which is allowed to use idf
        req.close();
    }

    @Test
    public void testIDFIsUsedInUserQueryIfSimilarityIsSetToDFC() {
        String q = "qup";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1^10 f2^2",
                QueryParsing.OP, "OR",
                USER_QUERY_SIMILARITY_SCORE, SIMILARITY_SCORE_DFC,
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("uq.similarityScore=off failed",
                req,
                "//result[@name='response'][@numFound='2']",
                "//str[@name='1'][contains(.,'idf')]",
                "//str[@name='2'][contains(.,'idf')]");
        req.close();
    }

    @Test
    public void testThatUserQueryBoostIsAppliedIfSimilarityIsOff() {
        String q = "qup";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1^10 f2^2",
                QueryParsing.OP, "OR",
                USER_QUERY_SIMILARITY_SCORE, SIMILARITY_SCORE_OFF,
                USER_QUERY_BOOST, "60.0",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("uq.boost failed with uq.similarityScore=off",
                req,
                "//result[@name='response'][@numFound='2']",
                "//str[@name='1'][not(contains(.,'idf'))]",
                "//str[@name='1'][contains(.,'600.0 = product of:')]",
                "//str[@name='1'][contains(.,'60.0 = queryBoost')]",
                "//str[@name='1'][contains(.,'10.0 = fieldBoost')]",
                "//str[@name='2'][contains(.,'600.0 = product of:')]",
                "//str[@name='2'][contains(.,'60.0 = queryBoost')]",
                "//str[@name='2'][contains(.,'10.0 = fieldBoost')]",
                "//str[@name='2'][contains(.,'idf')]", // due to the u100 boost up rule
                "//str[@name='2'][contains(.,'200.0 = boost')]"); // UP(100) * 2 (field weight)
        req.close();
    }

    @Test
    public void testThatUserQueryBoostIsAppliedIfSimilarityIsDFC() {
        String q = "qup";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1^10 f2^2",
                QueryParsing.OP, "OR",
                USER_QUERY_SIMILARITY_SCORE, SIMILARITY_SCORE_DFC,
                USER_QUERY_BOOST, "70.0",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("uq.boost failed with uq.similarityScore=off",
                req,
                "//result[@name='response'][@numFound='2']",
                "//str[@name='1'][contains(.,'idf')]",
                "//str[@name='1'][contains(.,'700.0 = boost')]", // uq.boost=70 * f1^10
                "//str[@name='2'][contains(.,'idf')]",
                "//str[@name='2'][contains(.,'700.0 = boost')]", // uq.boost=70 * f1^10
                "//str[@name='2'][contains(.,'200.0 = boost')]"); // UP(100)* f2^2
        req.close();
    }

    @Test
    public void testThatBoostUpWeightIsAppliedIfSimilarityIsDFC() {
        String q = "qup";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1^10 f2^2",
                QueryParsing.OP, "OR",
                USER_QUERY_SIMILARITY_SCORE, SIMILARITY_SCORE_DFC,
                USER_QUERY_BOOST, "80.0",
                QBOOST_WEIGHT, "3",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("qboost.weight failed with uq.similarityScore=dfc",
                req,
                "//result[@name='response'][@numFound='2']",
                "//str[@name='1'][contains(.,'idf')]",
                "//str[@name='1'][contains(.,'800.0 = boost')]", // uq.boost=80 * f1^10
                "//str[@name='2'][contains(.,'idf')]",
                "//str[@name='2'][contains(.,'800.0 = boost')]", // uq.boost=80 * f1^10
                "//str[@name='2'][contains(.,'600.0 = boost')]"); // UP(100)* f2^2 * qboost.weight=3
        req.close();
    }

    @Test
    public void testThatBoostUpWeightIsAppliedIfSimilarityIsOff() {
        String q = "qup";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1^10 f2^2",
                QueryParsing.OP, "OR",
                USER_QUERY_SIMILARITY_SCORE, SIMILARITY_SCORE_OFF,
                USER_QUERY_BOOST, "80.0",
                QBOOST_WEIGHT, "3",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("qboost.weight failed with uq.similarityScore=off",
                req,
                "//result[@name='response'][@numFound='2']",
                "//str[@name='1'][not(contains(.,'idf'))]", // similarity is off for user query
                "//str[@name='1'][contains(.,'800.0 = product of')]",
                "//str[@name='1'][contains(.,'80.0 = queryBoost')]", // uq.boost=80
                "//str[@name='1'][contains(.,'10.0 = fieldBoost')]", // f1^10
                "//str[@name='2'][contains(.,'idf')]", // similarity is on for boost
                "//str[@name='2'][contains(.,'800.0 = product of')]",
                "//str[@name='2'][contains(.,'80.0 = queryBoost')]", // uq.boost=80
                "//str[@name='2'][contains(.,'10.0 = fieldBoost')]", // f1^10
                "//str[@name='2'][contains(.,'600.0 = boost')]"); // UP(100)* f2^2 * qboost.weight=3
        req.close();
    }

    @Test
    public void testThatNegBoostIsAppliedIfSimilarityIsDFC() {
        String q = "m";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1^10 f2^2",
                QueryParsing.OP, "OR",
                USER_QUERY_SIMILARITY_SCORE, SIMILARITY_SCORE_DFC,
                USER_QUERY_BOOST, "80.0",
                QBOOST_NEG_WEIGHT, "3",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("qboost.negWeight failed with uq.similarityScore=dfc",
                req,
                "//result[@name='response'][@numFound='1']",
                "//str[@name='3'][contains(.,'idf')]",
                "//str[@name='3'][contains(.,'-1200000.0 = boost')]"); // qboost.negWeight=3*DOWN(200000) * f2^2
        req.close();
    }

    @Test
    public void testThatNegBoostIsAppliedIfSimilarityIsOff() {
        String q = "m";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1^10 f2^2",
                QueryParsing.OP, "OR",
                USER_QUERY_SIMILARITY_SCORE, SIMILARITY_SCORE_OFF,
                USER_QUERY_BOOST, "80.0",
                QBOOST_NEG_WEIGHT, "3",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("qboost.negWeight failed with uq.similarityScore=off",
                req,
                "//result[@name='response'][@numFound='1']",
                "//str[@name='3'][contains(.,'idf')]",
                "//str[@name='3'][contains(.,'-1200000.0 = boost')]"); // qboost.negWeight=3*DOWN(200000) * f2^2
        req.close();
    }

}
