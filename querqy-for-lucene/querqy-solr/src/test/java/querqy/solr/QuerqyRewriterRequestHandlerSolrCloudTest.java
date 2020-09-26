package querqy.solr;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;

import org.apache.lucene.util.TestUtil;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.cloud.AbstractDistribZkTestBase;
import org.apache.solr.cloud.SolrCloudTestCase;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.search.QueryParsing;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.solr.RewriterConfigRequestBuilder.SaveRewriterConfigSolrResponse;
import querqy.solr.rewriter.replace.ReplaceConfigRequestBuilder;
import querqy.solr.rewriter.commonrules.CommonRulesConfigRequestBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

@SolrTestCaseJ4.SuppressSSL
public class QuerqyRewriterRequestHandlerSolrCloudTest extends SolrCloudTestCase {

    final static String COLLECTION = "basic1";

    /** A basic client for operations at the cloud level, default collection will be set */
    private static CloudSolrClient CLOUD_CLIENT;

    /** One client per node */
    private static final ArrayList<HttpSolrClient> CLIENTS = new ArrayList<>(5);


    @BeforeClass
    public static void setupCluster() throws Exception {

        configureCluster(4)
                .addConfig("basic", getFile("solrcloud").toPath().resolve("configsets").resolve("basic")
                        .resolve("conf"))
                .configure();

        CollectionAdminRequest.createCollection(COLLECTION, "basic", 2, 1).process(cluster.getSolrClient());
        cluster.waitForActiveCollection(COLLECTION, 2, 2);

        CLOUD_CLIENT = cluster.getSolrClient();
        CLOUD_CLIENT.setDefaultCollection(COLLECTION);

        waitForRecoveriesToFinish(CLOUD_CLIENT);

        for (JettySolrRunner jetty : cluster.getJettySolrRunners()) {
            CLIENTS.add(getHttpSolrClient(jetty.getBaseUrl() + "/" + COLLECTION + "/"));
        }

    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (CLOUD_CLIENT != null) {
            CLOUD_CLIENT.close();
            CLOUD_CLIENT = null;
        }
        for (final HttpSolrClient client : CLIENTS) {
            client.close();
        }
        CLIENTS.clear();
    }

    @Before
    public void setUp() throws Exception {

        super.setUp();

        final SolrClient randClient = getRandClient();
        randClient.deleteByQuery("*:*");
        randClient.commit();

        randClient.add(Arrays.asList(
                sdoc("id", "1", "f1", "a"),
                sdoc("id", "2", "f2", "b"),
                sdoc("id", "3", "f2", "c")
        ));

        randClient.commit();

    }

    @Test
    public void testSimpleRequest() throws IOException, SolrServerException {
        final SolrParams params = params("collection", COLLECTION, "q", "*:*", "rows", "10", "defType", "querqy");
        final QueryRequest request = new QueryRequest(params);
        QueryResponse rsp = request.process(getRandClient(random()));
        assertEquals(3L, rsp.getResults().getNumFound());
    }

    @Test
    public void testSaveAndUpdateRewriter() throws IOException, SolrServerException {

        final SaveRewriterConfigSolrResponse response = new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: b").buildSaveRequest("rewriter_test_save").process(getRandClient());

        assertEquals(0, response.getStatus());

        final SolrParams params = params("collection", COLLECTION,
                "q", "a",
                "defType", "querqy",
                PARAM_REWRITERS, "rewriter_test_save",
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR"
                );

        QueryResponse rsp = new QueryRequest(params).process(getRandClient());
        assertEquals(2L, rsp.getResults().getNumFound());

        final SaveRewriterConfigSolrResponse response2 = new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: b\n SYNONYM: c")
                .buildSaveRequest("rewriter_test_save").process(getRandClient());

        assertEquals(0, response2.getStatus());

        QueryResponse rsp2 = new QueryRequest(params).process(getRandClient());
        assertEquals(3L, rsp2.getResults().getNumFound());

    }

    @Test
    public void testRewriteChain() throws IOException, SolrServerException {

        assertEquals(0, new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: b").buildSaveRequest("chain_common_rules").process(getRandClient())
                .getStatus());

        assertEquals(0, new ReplaceConfigRequestBuilder()
                .rules("sd => a").buildSaveRequest("chain_replace").process(getRandClient())
                .getStatus());

        // common rules rewriter only
        QueryResponse rsp = new QueryRequest(
                params("collection", COLLECTION,
                        "q", "a",
                        "defType", "querqy",
                        PARAM_REWRITERS, "chain_common_rules",
                        DisMaxParams.QF, "f1 f2",
                        QueryParsing.OP, "OR")

        ).process(getRandClient());
        assertEquals(2L, rsp.getResults().getNumFound());

        // replace rewriter first, supplies input to following common rules rewriter
        QueryResponse rsp2 = new QueryRequest(
                params("collection", COLLECTION,
                        "q", "sd",
                        "defType", "querqy",
                        PARAM_REWRITERS, "chain_replace,chain_common_rules",
                        DisMaxParams.QF, "f1 f2",
                        QueryParsing.OP, "OR")

        ).process(getRandClient());
        assertEquals(2L, rsp2.getResults().getNumFound());

        // common rules first (but no match), followed by replace rewriter
        QueryResponse rsp3 = new QueryRequest(
                params("collection", COLLECTION,
                        "q", "sd",
                        "defType", "querqy",
                        PARAM_REWRITERS, "chain_common_rules,chain_replace",
                        DisMaxParams.QF, "f1 f2",
                        QueryParsing.OP, "OR")

        ).process(getRandClient());
        assertEquals(1L, rsp3.getResults().getNumFound());

    }


    @Test
    public void testDeleteRewriter() throws IOException, SolrServerException {

        assertEquals(0, new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: b").buildSaveRequest("delete_common_rules")
                .process(getRandClient())
                .getStatus());

        // common rules rewriter only
        QueryResponse rsp = new QueryRequest(
                params("collection", COLLECTION,
                        "q", "a",
                        "defType", "querqy",
                        PARAM_REWRITERS, "delete_common_rules",
                        DisMaxParams.QF, "f1 f2",
                        QueryParsing.OP, "OR")

        ).process(getRandClient());
        assertEquals(2L, rsp.getResults().getNumFound());

        assertEquals(0, RewriterConfigRequestBuilder.buildDeleteRequest("delete_common_rules")
                .process(getRandClient())
                .getStatus());

        try {
            new QueryRequest(
                    params("collection", COLLECTION,
                            "q", "a",
                            "defType", "querqy",
                            PARAM_REWRITERS, "delete_common_rules",
                            DisMaxParams.QF, "f1 f2",
                            QueryParsing.OP, "OR")

            ).process(getRandClient());
            fail("Expected bad request exception for deleted rewriter");
        } catch (final SolrException e) {
            assertEquals(SolrException.ErrorCode.BAD_REQUEST.code, e.code());
        }

    }

    @Test
    public void testLargeConfig() throws IOException, SolrServerException, InterruptedException {

        // we upload a 2.3 MB rules.txt, which can be compressed to < 1 MB
        assertEquals(0, new CommonRulesConfigRequestBuilder()
                .rules(getClass().getClassLoader().getResourceAsStream("configs/commonrules/rules-large.txt"))
                .buildSaveRequest("large_common_rules")
                .process(getRandClient())
                .getStatus());

        // It will take a bit to propagate this large config to the nodes. We try to apply the rewriter max. 3 times and
        // wait for a bit between the attempts
        int attempts = 3;
        QueryResponse rsp = null;
        do {
            synchronized (this) {
                wait(800L);
            }
            try {
                rsp = new QueryRequest(
                        params("collection", COLLECTION,
                                "q", "f",
                                "defType", "querqy",
                                PARAM_REWRITERS, "large_common_rules",
                                DisMaxParams.QF, "f1 f2",
                                QueryParsing.OP, "OR")

                ).process(getRandClient());
                attempts = 0;
            } catch (Exception e) {
                if ((attempts <= 1) || (!e.getMessage().contains("No such rewriter"))){
                    throw e;
                }
                attempts--;
            }
        } while (attempts > 0);

        assertNotNull(rsp);
        assertEquals(1L, rsp.getResults().getNumFound());

    }

    private SolrClient getRandClient() {
        return getRandClient(random());
    }

    /**
     * returns a random SolrClient -- either a CloudSolrClient, or an HttpSolrClient pointed
     * at a node in our cluster
     */
    public static SolrClient getRandClient(Random rand) {
        int numClients = CLIENTS.size();
        int idx = TestUtil.nextInt(rand, 0, numClients);

        return (idx == numClients) ? CLOUD_CLIENT : CLIENTS.get(idx);
    }

    public static void waitForRecoveriesToFinish(CloudSolrClient client) throws Exception {
        assert null != client.getDefaultCollection();
        AbstractDistribZkTestBase.waitForRecoveriesToFinish(client.getDefaultCollection(), client.getZkStateReader(),
                true, true, 330);
    }

}
