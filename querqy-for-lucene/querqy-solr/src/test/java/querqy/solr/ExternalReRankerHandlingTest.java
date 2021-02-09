package querqy.solr;

import static querqy.solr.QuerqyDismaxParams.QBOOST_METHOD;
import static querqy.solr.QuerqyDismaxParams.QBOOST_METHOD_RERANK;
import static querqy.solr.QuerqyDismaxParams.QBOOST_RERANK_NUMDOCS;
import static querqy.solr.QuerqyDismaxParams.QRQ;
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
public class ExternalReRankerHandlingTest extends SolrTestCaseJ4 {

    // org.apache.lucene.search.QueryRescorer#explain adds re-scorer classname in description
    private static final String ASSERT_NO_RERANK_APPLIED = "//lst[@name='explain']/str[not(contains(.,'ReRank'))]";
    private static final String ASSERT_SOLR_RERANK_APPLIED = "//lst[@name='explain']/str[contains(.,'solr.search.ReRankQParserPlugin$ReRankQueryRescorer')]";
    private static final String ASSERT_NO_QUERQY_RERANK_APPLIED = "//lst[@name='explain']/str[not(contains(.,'QuerqyReRankQuery'))]";
    private static final String ASSERT_QUERQY_RERANK_APPLIED = "//lst[@name='explain']/str[contains(.,'QuerqyReRankQuery')]";

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
    public void testSolrReRankingParameter() {
        index(d("id", "1", "f1", "term a"),
                d("id", "2", "f1", "term b", "f2", "term"));
        String q = "term";

        // basic query with tie = 1 to prefer multiple matches
        SolrQueryRequest reqWithoutReRanking = req("q", q,
                DisMaxParams.QF, "f1 f2",
                "defType", "querqy",
                "tie", "1",
                "sort", "score DESC, id ASC",
                PARAM_REWRITERS, "common_rules",
                "debug", "true");

        // same as above but with Solr rq/rqq reranking
        SolrQueryRequest reqReRanking = req("q", q,
                DisMaxParams.QF, "f1 f2",
                "defType", "querqy",
                "tie", "1",
                "sort", "score DESC, id ASC",
                "rq", "{!rerank reRankQuery=$rqq reRankDocs=2 reRankWeight=100}",
                "rqq", "f1:a",
                PARAM_REWRITERS, "common_rules",
                "debug", "true");

        // doc 2 gets a better as query matches in two fields and tie = 1
        assertQ("Result should be sorted by doc with more matches",
                reqWithoutReRanking,
                "//result[@name='response'][@numFound='2']",
                "//doc[1]/str[@name='id'][text()='2']",
                "//doc[2]/str[@name='id'][text()='1']",
                ASSERT_NO_RERANK_APPLIED);


        // doc 1 wins as it is reranked by rqq=a
        assertQ("Result should be sorted with Solr rank query",
                reqReRanking,
                "//result[@name='response'][@numFound='2']",
                "//doc[1]/str[@name='id'][text()='1']",
                "//doc[2]/str[@name='id'][text()='2']",
                ASSERT_SOLR_RERANK_APPLIED);

        reqWithoutReRanking.close();
        reqReRanking.close();
    }

    @Test
    public void testQuerqyReRankingParameterIsApplied() {
        index(d("id", "1", "f1", "term a"),
                d("id", "2", "f1", "term b", "f2", "term"));
        String q = "term";

        // same as with Solr's rerank parameter processing, doc 1 comes first despite doc2 having more matches
        SolrQueryRequest query = req("q", q,
                DisMaxParams.QF, "f1 f2",
                "defType", "querqy",
                "tie", "1",
                "sort", "score DESC, id ASC",
                QRQ, "{!rerank reRankQuery=$rqq reRankDocs=2 reRankWeight=100}",
                "rqq", "f1:a",
                PARAM_REWRITERS, "common_rules",
                "debug", "true");

        // doc 1 wins as it is reranked by rqq=a
        assertQ("Result should be sorted with rerank query",
                query,
                "//result[@name='response'][@numFound='2']",
                "//doc[1]/str[@name='id'][text()='1']",
                "//doc[2]/str[@name='id'][text()='2']",
                ASSERT_SOLR_RERANK_APPLIED);

        query.close();
    }

    @Test
    public void testQuerqyBoostingWinsOverQuerqyRerankParameter() {
        index(d("id", "1", "f1", "qup", "f2", "qup x"),
                d("id", "2", "f1", "qup otherterm", "f2", "u100"));

        // query with rules-based boosted term, qup has UP(100):u100
        String q = "qup";

        // boost added to main query
        SolrQueryRequest reqWithBoostOnMainQueryAndNoSolrReRanking = req("q", q,
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR",
                "tie", "1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        // same as above but with additional Solr rq/rqq
        SolrQueryRequest reqWithBoostOnMainQueryAndSolrReRanking = req("q", q,
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR",
                "tie", "1",
                "defType", "querqy",
                "debugQuery", "true",
                "rq", "{!rerank reRankQuery=$rqq reRankDocs=2 reRankWeight=100}",
                "rqq", "f2:x",
                PARAM_REWRITERS, "common_rules");

        // same as above but with Querqy rq/rqq instead
        SolrQueryRequest reqWithBoostOnMainQueryAndQuerqyReRankParameter = req("q", q,
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR",
                "tie", "1",
                "defType", "querqy",
                "debugQuery", "true",
                QRQ, "{!rerank reRankQuery=$rqq reRankDocs=2 reRankWeight=100}",
                "rqq", "f2:x",
                PARAM_REWRITERS, "common_rules");

        // boost added as QuerqyReRankQuery
        SolrQueryRequest reqWithBoostAndRerankMethodAndNoReRankParameter = req("q", q,
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR",
                "tie", "1",
                QBOOST_METHOD, QBOOST_METHOD_RERANK,
                QBOOST_RERANK_NUMDOCS, "2",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        // same as above but with Solr rq/rqq
        SolrQueryRequest reqWithBoostAndRerankMethodAndSolrReRankingParameter = req("q", q,
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR",
                "tie", "1",
                QBOOST_METHOD, QBOOST_METHOD_RERANK,
                QBOOST_RERANK_NUMDOCS, "2",
                "defType", "querqy",
                "debugQuery", "true",
                "rq", "{!rerank reRankQuery=$rqq reRankDocs=2 reRankWeight=100}",
                "rqq", "f2:x",
                PARAM_REWRITERS, "common_rules");

        // same as above but with Querqy rq/rqq
        SolrQueryRequest reqWithBoostAndRerankMethodAndQuerqyReRankParameter = req("q", q,
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR",
                "tie", "1",
                QBOOST_METHOD, QBOOST_METHOD_RERANK,
                QBOOST_RERANK_NUMDOCS, "2",
                "defType", "querqy",
                "debugQuery", "true",
                "querqy.rq", "{!rerank reRankQuery=$rqq reRankDocs=2 reRankWeight=100}",
                "rqq", "f2:x",
                PARAM_REWRITERS, "common_rules");

        assertQ("Result should be sorted by boosted term by an additional SHOULD clause on the main query, not by reranking",
                reqWithBoostOnMainQueryAndNoSolrReRanking,
                "//result[@name='response'][@numFound='2']",
                "//doc[1]/str[@name='id'][text()='2']",
                "//doc[2]/str[@name='id'][text()='1']",
                ASSERT_NO_RERANK_APPLIED);

        assertQ("Result should be sorted by external Solr reranking",
                reqWithBoostOnMainQueryAndSolrReRanking,
                "//result[@name='response'][@numFound='2']",
                "//doc[1]/str[@name='id'][text()='1']",
                "//doc[2]/str[@name='id'][text()='2']",
                ASSERT_SOLR_RERANK_APPLIED,
                ASSERT_NO_QUERQY_RERANK_APPLIED);

        assertQ("Result should be sorted by boosted term by an additional SHOULD clause on the main query, rerank parameter being ignored",
                reqWithBoostOnMainQueryAndQuerqyReRankParameter,
                "//result[@name='response'][@numFound='2']",
                "//doc[1]/str[@name='id'][text()='2']",
                "//doc[2]/str[@name='id'][text()='1']",
                ASSERT_NO_RERANK_APPLIED);

        assertQ("Result should be sorted by boosted term by a Querqy rerank query",
                reqWithBoostAndRerankMethodAndNoReRankParameter,
                "//result[@name='response'][@numFound='2']",
                "//doc[1]/str[@name='id'][text()='2']",
                "//doc[2]/str[@name='id'][text()='1']",
                ASSERT_QUERQY_RERANK_APPLIED);

        assertQ("Result should be sorted by external Solr reranking",
                reqWithBoostAndRerankMethodAndSolrReRankingParameter,
                "//result[@name='response'][@numFound='2']",
                "//doc[1]/str[@name='id'][text()='1']",
                "//doc[2]/str[@name='id'][text()='2']",
                ASSERT_SOLR_RERANK_APPLIED,
                ASSERT_NO_QUERQY_RERANK_APPLIED);

        assertQ("Result should be sorted by boosted term by external Querqy reranking",
                reqWithBoostAndRerankMethodAndQuerqyReRankParameter,
                "//result[@name='response'][@numFound='2']",
                "//doc[1]/str[@name='id'][text()='2']",
                "//doc[2]/str[@name='id'][text()='1']",
                ASSERT_QUERQY_RERANK_APPLIED);

        reqWithBoostOnMainQueryAndNoSolrReRanking.close();
        reqWithBoostOnMainQueryAndSolrReRanking.close();
        reqWithBoostOnMainQueryAndQuerqyReRankParameter.close();

        reqWithBoostAndRerankMethodAndNoReRankParameter.close();
        reqWithBoostAndRerankMethodAndSolrReRankingParameter.close();
        reqWithBoostAndRerankMethodAndQuerqyReRankParameter.close();
    }

    private void index(String[]... docs) {
        for (String[] doc: docs) {
            assertU(adoc(doc));
        }
        assertU(commit());
    }

    private static String[] d(String... strings) {
        return strings;
    }


}
