package querqy.solr;

import static querqy.solr.QuerqyDismaxParams.QBOOST_NEG_WEIGHT;
import static querqy.solr.QuerqyDismaxParams.QBOOST_SIMILARITY_SCORE;
import static querqy.solr.QuerqyDismaxParams.QBOOST_WEIGHT;
import static querqy.solr.QuerqyDismaxParams.SIMILARITY_SCORE_DFC;
import static querqy.solr.QuerqyDismaxParams.SIMILARITY_SCORE_OFF;
import static querqy.solr.QuerqyDismaxParams.SIMILARITY_SCORE_ON;
import static querqy.solr.QuerqyDismaxParams.USER_QUERY_BOOST;
import static querqy.solr.QuerqyDismaxParams.USER_QUERY_SIMILARITY_SCORE;
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
public class MainQueryBoostTest extends SolrTestCaseJ4 {

    public void index() {

        assertU(adoc("id", "1", "f1", "qup"));
        assertU(adoc("id", "2", "f1", "qup other", "f2", "u100"));
        assertU(adoc("id", "3", "f1", "m", "f2", "d"));
        assertU(adoc("id", "4", "f1", "qdown1 d2"));
        assertU(adoc("id", "5", "f1", "qdown2 d1"));
        assertU(adoc("id", "6", "f1", "qx1", "f2", "1u10 1d10"));
        assertU(adoc("id", "7", "f1", "qx", "f2", "2u10 2d10"));
        assertU(commit());
    }

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig.xml", "schema.xml");
        withCommonRulesRewriter(h.getCore(), "common_rules", "configs/commonrules/rules.txt");
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
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("uq.similarityScore=off failed",
                req,
                "//result[@name='response'][@numFound='2']",
                "//str[@name='1'][not(contains(.,'idf'))]",  // no idf for user query
                "//str[@name='2'][not(contains(.,'idf'))]",  // no idf for user query
                // boost query score not ending in .0, indicating that similarity was used
                "//str[@name='2'][not(substring(.,string-length(.) - 2) = '0)')]");

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
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules"
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
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR",
                USER_QUERY_SIMILARITY_SCORE, SIMILARITY_SCORE_OFF,
                USER_QUERY_BOOST, "217.3",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("uq.boost failed with uq.similarityScore=off",
                req,
                "//result[@name='response'][@numFound='2']",
                "//doc[1]/str[@name='id'][contains(.,'2')]",
                "//doc[2]/str[@name='id'][contains(.,'1')]",
                "//str[@name='1'][contains(.,'217.3 = weight(f1:qup')]",
                "//str[@name='2'][contains(.,'217.3 = weight(f1:qup')]");
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
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("uq.boost failed with uq.similarityScore=dfc",
                req,
                "//result[@name='response'][@numFound='2']",
                "//str[@name='1'][contains(.,'idf')]",
                // uq.boost=70 * f1^10 * (1.2 + 1) // BM25Similarity.k1 +1:
                "//str[@name='1'][contains(.,'700.0 = boost')]",
                "//str[@name='1'][contains(.,'0.0 = FunctionQuery(AdditiveBoostFunction(100.0,query(+(f1:u100^10.0 | " +
                        "f2:u100^2.0),def=0.0))')] or //str[@name='1'][contains(.,'0.0 = FunctionQuery(" +
                        "AdditiveBoostFunction(100.0,query(+(f2:u100^2.0 | f1:u100^10.0),def=0.0))')]",
                "//str[@name='2'][contains(.,'idf')]",
                "//str[@name='2'][contains(.,'700.0 = boost')]", // uq.boost=70 * f1^10 * (1.2 + 1)
                "//str[@name='2'][not(contains(.,'AdditiveBoostFunction(100.0,query(+(f1:u100^10.0 | f2:u100^2.0)," +
                        "def=0.0)=0.0'))] or //str[@name='2'][not(contains(.,'" +
                        "AdditiveBoostFunction(100.0,query(+(f2:u100^2.0 | f1:u100^10.0),def=0.0)=0.0'))]",
                "//str[@name='2'][contains(.,'AdditiveBoostFunction(100.0,query(+(f1:u100^10.0 | f2:u100^2.0)," +
                        "def=0.0)')] or //str[@name='2'][contains(.,'" +
                        "AdditiveBoostFunction(100.0,query(+(f2:u100^2.0 | f1:u100^10.0),def=0.0)')]");
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
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("qboost.weight failed with uq.similarityScore=dfc",
                req,
                "//result[@name='response'][@numFound='2']",
                "//str[@name='1'][contains(.,'idf')]",
                "//str[@name='1'][contains(.,'800.0 = boost')]", // uq.boost=80 * f1^10
                "//str[@name='2'][contains(.,'idf')]",
                "//str[@name='2'][contains(.,'800.0 = boost')]", // uq.boost=80 * f1^10
                // 300 = UP(100) * qboost.weight=3
                "//str[@name='2'][contains(.,'AdditiveBoostFunction(300.0,query(+(f1:u100^10.0 | f2:u100^2.0)," +
                        "def=0.0)')] or //str[@name='2'][contains(.,'" +
                        "AdditiveBoostFunction(300.0,query(+(f2:u100^2.0 | f1:u100^10.0),def=0.0)')]",
                "//str[@name='2'][not(contains(.,'AdditiveBoostFunction(300.0,query(+(f1:u100^10.0 | f2:u100^2.0)," +
                        "def=0.0)=0'))] or //str[@name='2'][not(contains(.,'" +
                        "AdditiveBoostFunction(300.0,query(+(f2:u100^2.0 | f1:u100^10.0),def=0.0)=0'))]");
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
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("qboost.weight failed with uq.similarityScore=off",
                req,
                "//result[@name='response'][@numFound='2']",
                "//str[@name='1'][not(contains(.,'idf'))]", // similarity is off for user query
                "//str[@name='1'][contains(.,'800.0 = product of')]",
                "//str[@name='1'][contains(.,'80.0 = queryBoost')]", // uq.boost=80
                "//str[@name='1'][contains(.,'10.0 = fieldBoost')]", // f1^10
                "//str[@name='1'][contains(.,'0.0 = AdditiveBoostFunction(300.0,query(+(f1:u100^10.0 | f2:u100^2.0)," +
                        "def=0.0)=0.0)')] or //str[@name='1'][contains(.,'0.0 = " +
                        "AdditiveBoostFunction(300.0,query(+(f2:u100^2.0 | f1:u100^10.0),def=0.0)=0.0)')]",

                // boost = (QBOOST_WEIGHT = 3) * UP(100) = 300 * similarity score (should not end in .0)
                "//str[@name='2'][contains(.,'AdditiveBoostFunction(300.0,query(+(f1:u100^10.0 | f2:u100^2.0)," +
                        "def=0.0)')] or //str[@name='2'][contains(.,'" +
                        "AdditiveBoostFunction(300.0,query(+(f2:u100^2.0 | f1:u100^10.0),def=0.0)')]",
                "//str[@name='2'][not(substring(.,string-length(.) - 2) = '0)')]",
                // similarity is on for boost
                "//str[@name='2'][contains(.,'800.0 = product of')]",
                "//str[@name='2'][contains(.,'80.0 = queryBoost')]", // uq.boost=80
                "//str[@name='2'][contains(.,'10.0 = fieldBoost')]"); // f1^10
        req.close();
    }

    @Test
    public void testThatNegBoostIsAppliedIfSimilarityIsDFC() {
        String q = "m u100";
//        m =>
//        DOWN(200000): d

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1^10 f2^2",
                QueryParsing.OP, "OR",
                USER_QUERY_SIMILARITY_SCORE, SIMILARITY_SCORE_DFC,
                USER_QUERY_BOOST, "80.0",
                QBOOST_NEG_WEIGHT, "3",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("qboost.negWeight failed with uq.similarityScore=dfc",
                req,
                "//result[@name='response'][@numFound='2']",
                "//str[1][@name='2'][contains(.,'idf')]",
                "//str[1][@name='2'][contains(.,'600000.0 = FunctionQuery')]", // qboost.negWeight=3*DOWN(200000) added

                "//str[2][@name='3'][contains(.,'idf')]",
                "//str[2][@name='3'][not(contains(.,'60000.0 = FunctionQuery'))]");
        req.close();
    }

    @Test
    public void testThatNegBoostIsAppliedIfSimilarityIsOff() {
        String q = "m u100";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1^100 f2^2",
                QueryParsing.OP, "OR",
                USER_QUERY_SIMILARITY_SCORE, SIMILARITY_SCORE_OFF,
                USER_QUERY_BOOST, "80.0",
                QBOOST_NEG_WEIGHT, "3",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("qboost.negWeight failed with uq.similarityScore=off",
                req,
                "//result[@name='response'][@numFound='2']",
                "//doc[1]/str[@name='id'][contains(.,'2')]",
                "//doc[2]/str[@name='id'][contains(.,'3')]",
                // (qboost.negWeight=3) * DOWN(200000) = 60000 -> default if doc doesn't match the down query
                "//str[@name='2'][contains(.,'600000.0 = AdditiveBoostFunction(-600000.0,query(+(f1:d^100.0 f2:d^2.0)," +
                        "def=0.0)=0.0)')]",
                "//str[@name='2'][contains(.,'80.0 = queryBoost')]",
                "//str[@name='2'][contains(.,'2.0 = fieldBoost')]",
                "//str[@name='3'][contains(.,'AdditiveBoostFunction(-600000.0,query(+(f1:d^100.0 f2:d^2.0),def=0.0))')]",
                "//str[@name='3'][not(contains(.,'AdditiveBoostFunction(-600000.0,query(+(f1:d^100.0 f2:d^2.0)," +
                        "def=0.0)=0.0))'))]",
                "//str[@name='3'][not(contains(.,'-600000.0 = AdditiveBoostFunction'))]",
                "//str[@name='3'][contains(.,'80.0 = queryBoost')]",
                "//str[@name='3'][contains(.,'100.0 = fieldBoost')]");
        req.close();
    }

    @Test
    public void testThatNegBoostIsGradedIfSimilarityIsOff() {
        String q = "qdown1 qdown2";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                QueryParsing.OP, "OR",
                USER_QUERY_SIMILARITY_SCORE, SIMILARITY_SCORE_OFF,
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("graded down failed with uq.similarityScore=off",
                req,
                "//result[@name='response'][@numFound='2']",
                "//doc[1]/str[@name='id'][contains(.,'5')]",
                "//doc[2]/str[@name='id'][contains(.,'4')]",
                "//str[@name='5'][contains(.,'0.2 = AdditiveBoostFunction(-0.2,query(+f1:d2,def=0.0)=0.0)')]",
                "//str[@name='5'][contains(.,'AdditiveBoostFunction(-0.1,query(+f1:d1,def=0.0)')]",
                "//str[@name='5'][not(contains(.,'AdditiveBoostFunction(-0.1,query(+f1:d1,def=0.0)=0.0'))]",
                "//str[@name='4'][contains(.,'AdditiveBoostFunction(-0.2,query(+f1:d2,def=0.0)')]",
                "//str[@name='4'][not(contains(.,'AdditiveBoostFunction(-0.2,query(+f1:d2,def=0.0)=0.0'))]",
                "//str[@name='4'][contains(.,'AdditiveBoostFunction(-0.1,query(+f1:d1,def=0.0)=0.0)')]"
        );
        req.close();
    }

    @Test
    public void testThatNegBoostIsGradedIfSimilarityIsOn() {
        String q = "qdown1 qdown2";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                QueryParsing.OP, "OR",
                USER_QUERY_SIMILARITY_SCORE, SIMILARITY_SCORE_ON,
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("graded down failed with uq.similarityScore=off",
                req,
                "//result[@name='response'][@numFound='2']",
                "//doc[1]/str[@name='id'][contains(.,'5')]",
                "//doc[2]/str[@name='id'][contains(.,'4')]",
                "//str[@name='5'][contains(.,'0.2 = AdditiveBoostFunction(-0.2,query(+f1:d2,def=0.0)=0.0)')]",
                "//str[@name='5'][contains(.,'AdditiveBoostFunction(-0.1,query(+f1:d1,def=0.0)')]",
                "//str[@name='5'][not(contains(.,'AdditiveBoostFunction(-0.1,query(+f1:d1,def=0.0)=0.0'))]",
                "//str[@name='4'][contains(.,'AdditiveBoostFunction(-0.2,query(+f1:d2,def=0.0)')]",
                "//str[@name='4'][not(contains(.,'AdditiveBoostFunction(-0.2,query(+f1:d2,def=0.0)=0.0'))]",
                "//str[@name='4'][contains(.,'0.1 = AdditiveBoostFunction(-0.1,query(+f1:d1,def=0.0)=0.0)')]"
        );
        req.close();
    }

    @Test
    public void testThatNegBoostIsGradedIfSimilarityIsDfc() {
        String q = "qdown1 qdown2";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                QueryParsing.OP, "OR",
                USER_QUERY_SIMILARITY_SCORE, SIMILARITY_SCORE_DFC,
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("graded down failed with uq.similarityScore=off",
                req,
                "//result[@name='response'][@numFound='2']",
                "//doc[1]/str[@name='id'][contains(.,'5')]",
                "//doc[2]/str[@name='id'][contains(.,'4')]",
                "//str[@name='5'][contains(.,'0.2 = AdditiveBoostFunction(-0.2,query(+f1:d2,def=0.0)=0.0)')]",
                "//str[@name='5'][contains(.,'AdditiveBoostFunction(-0.1,query(+f1:d1,def=0.0)')]",
                "//str[@name='5'][not(contains(.,'AdditiveBoostFunction(-0.1,query(+f1:d1,def=0.0)=0.0'))]",
                "//str[@name='4'][contains(.,'AdditiveBoostFunction(-0.2,query(+f1:d2,def=0.0)')]",
                "//str[@name='4'][not(contains(.,'AdditiveBoostFunction(-0.2,query(+f1:d2,def=0.0)=0.0'))]",
                "//str[@name='4'][contains(.,'0.1 = AdditiveBoostFunction(-0.1,query(+f1:d1,def=0.0)=0.0)')]"
        );
        req.close();
    }

    @Test
    public void testThatNegBoostIsGradedIfBoostSimilarityIsOff() {
        String q = "qdown1 qdown2";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                QueryParsing.OP, "OR",
                QBOOST_SIMILARITY_SCORE, SIMILARITY_SCORE_OFF,
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("graded down failed with qboost.similarityScore=off",
                req,
                "//result[@name='response'][@numFound='2']",
                "//doc[1]/str[@name='id'][contains(.,'5')]",
                "//doc[2]/str[@name='id'][contains(.,'4')]",
                "//str[@name='5'][contains(.,'0.2 = AdditiveBoostFunction(-0.2,query(+f1:d2,def=0.0)=0.0)')]",
                "//str[@name='5'][contains(.,'0.05 = AdditiveBoostFunction(-0.1,query(+f1:d1,def=0.0)=1.0)')]",
                "//str[@name='4'][contains(.,'0.1 = AdditiveBoostFunction(-0.2,query(+f1:d2,def=0.0)=1.0)')]",
                "//str[@name='4'][contains(.,'0.1 = AdditiveBoostFunction(-0.1,query(+f1:d1,def=0.0)=0.0)')]"
        );
        req.close();
    }

    @Test
    public void testUpAndDown() {
        String q = "qx1 qx";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR",
                QBOOST_SIMILARITY_SCORE, SIMILARITY_SCORE_OFF,
                "defType", "querqy",
                "debugQuery", "true",
                "fl", "*,score",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("graded down failed with qboost.similarityScore=off",
                req,
                "//result[@name='response'][@numFound='2']",
                "//str[@name='6'][contains(.,'5.0 = AdditiveBoostFunction(10.0,query(+(f1:1u10 | f2:1u10)," +
                        "def=0.0)=1.0)')] or //str[@name='6'][contains(.,'5.0 = " +
                        "AdditiveBoostFunction(10.0,query(+(f2:1u10 | f1:1u10),def=0.0)=1.0)')]",
                "//str[@name='6'][contains(.,'5.0 = AdditiveBoostFunction(-10.0,query(+(f1:1d10 f2:1d10)," +
                        "def=0.0)=1.0)')] or //str[@name='6'][contains(.,'5.0 = " +
                        "AdditiveBoostFunction(-10.0,query(+(f2:1d10 f1:1d10),def=0.0)=1.0)')]",
                "//str[@name='7'][contains(.,'0.0 = AdditiveBoostFunction(10.0,query(+(f1:1u10 | f2:1u10)," +
                        "def=0.0)=0.0)')] or //str[@name='7'][contains(.,'0.0 = " +
                        "AdditiveBoostFunction(10.0,query(+(f2:1u10 | f1:1u10),def=0.0)=0.0)')]",
                "//str[@name='7'][contains(.,'10.0 = AdditiveBoostFunction(-10.0,query(+(f1:1d10 f2:1d10)," +
                        "def=0.0)=0.0)')] or //str[@name='7'][contains(.,'10.0 = " +
                        "AdditiveBoostFunction(-10.0,query(+(f2:1d10 f1:1d10),def=0.0)=0.0)')]"
        );
        req.close();
    }

}
