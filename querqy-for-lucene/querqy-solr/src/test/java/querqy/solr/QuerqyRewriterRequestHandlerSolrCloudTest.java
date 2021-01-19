package querqy.solr;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.RewriterConfigRequestBuilder.buildDeleteRequest;
import static querqy.solr.RewriterConfigRequestBuilder.buildGetRequest;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.search.QueryParsing;
import org.junit.*;
import querqy.solr.RewriterConfigRequestBuilder.GetRewriterConfigSolrResponse;
import querqy.solr.RewriterConfigRequestBuilder.SaveRewriterConfigSolrResponse;
import querqy.solr.rewriter.replace.ReplaceConfigRequestBuilder;
import querqy.solr.rewriter.commonrules.CommonRulesConfigRequestBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@SolrTestCaseJ4.SuppressSSL
public class QuerqyRewriterRequestHandlerSolrCloudTest extends AbstractQuerqySolrCloudTestCase {

    final static String COLLECTION = "basic1";

    /**
     * A basic client for operations at the cloud level, default collection will be set
     */
    private static CloudSolrClient CLOUD_CLIENT;

    /**
     * One client per node
     */
    private static final List<HttpSolrClient> CLIENTS = new ArrayList<>(5);


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

    @After
    public void cleanup() throws IOException {
        try {
            for (SolrClient c : CLIENTS) {
                buildDeleteRequest("chain_replace").process(c);
                buildDeleteRequest("chain_common_rules").process(c);
                buildDeleteRequest("conf_common_rules").process(c);
                buildDeleteRequest("rewriter_test_save").process(c);
            }
        } catch (SolrServerException | HttpSolrClient.RemoteSolrException e) {
            // nothing to do
        }
    }

    @Test
    public void testSimpleRequest() throws IOException, SolrServerException {
        final SolrParams params = params("collection", COLLECTION, "q", "*:*", "rows", "10", "defType", "querqy");
        final QueryRequest request = new QueryRequest(params);
        QueryResponse rsp = request.process(getRandClient());
        assertEquals(3L, rsp.getResults().getNumFound());
    }

    @Test
    public void testSaveAndUpdateRewriter() throws Exception {

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

        QueryResponse rsp = waitForRewriterAndQuery(params, getRandClient());

        assertEquals(2L, rsp.getResults().getNumFound());

        final SaveRewriterConfigSolrResponse response2 = new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: b\n SYNONYM: c")
                .buildSaveRequest("rewriter_test_save").process(getRandClient());

        assertEquals(0, response2.getStatus());

        QueryResponse rsp2 = waitForRewriterAndQuery(params, getRandClient());
        assertEquals(3L, rsp2.getResults().getNumFound());

    }

    @Test
    public void testRewriteChain() throws Exception {

        assertEquals(0, new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: b").buildSaveRequest("chain_common_rules").process(getRandClient())
                .getStatus());

        assertEquals(0, new ReplaceConfigRequestBuilder()
                .rules("sd => a").buildSaveRequest("chain_replace").process(getRandClient())
                .getStatus());

        // common rules rewriter only
        QueryResponse rsp = waitForRewriterAndQuery(

                params("collection", COLLECTION,
                        "q", "a",
                        "defType", "querqy",
                        PARAM_REWRITERS, "chain_common_rules",
                        DisMaxParams.QF, "f1 f2",
                        QueryParsing.OP, "OR"),
                getRandClient());

        assertEquals(2L, rsp.getResults().getNumFound());

        // replace rewriter first, supplies input to following common rules rewriter
        QueryResponse rsp2 = waitForRewriterAndQuery(
                params("collection", COLLECTION,
                        "q", "sd",
                        "defType", "querqy",
                        PARAM_REWRITERS, "chain_replace,chain_common_rules",
                        DisMaxParams.QF, "f1 f2",
                        QueryParsing.OP, "OR"),
                getRandClient());
        assertEquals(2L, rsp2.getResults().getNumFound());

        // common rules first (but no match), followed by replace rewriter
        QueryResponse rsp3 = waitForRewriterAndQuery(
                params("collection", COLLECTION,
                        "q", "sd",
                        "defType", "querqy",
                        PARAM_REWRITERS, "chain_common_rules,chain_replace",
                        DisMaxParams.QF, "f1 f2",
                        QueryParsing.OP, "OR"),
                getRandClient());
        assertEquals(1L, rsp3.getResults().getNumFound());

    }

    @Test
    public void testDeleteRewriter() throws Exception {

        assertEquals(0, new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: b").buildSaveRequest("delete_common_rules")
                .process(getRandClient())
                .getStatus());

        final SolrClient client = getRandClient();

        final QueryResponse rsp = waitForRewriterAndQuery(
                params("collection", COLLECTION,
                        "q", "a",
                        "defType", "querqy",
                        PARAM_REWRITERS, "delete_common_rules",
                        DisMaxParams.QF, "f1 f2",
                        QueryParsing.OP, "OR"),
                client);


        assertEquals(2L, rsp.getResults().getNumFound());

        assertEquals(0, buildDeleteRequest("delete_common_rules")
                .process(getRandClient())
                .getStatus());

        try {

            waitForRewriterAndQuery(
                    params("collection", COLLECTION,
                            "q", "a",
                            "defType", "querqy",
                            PARAM_REWRITERS, "delete_common_rules",
                            DisMaxParams.QF, "f1 f2",
                            QueryParsing.OP, "OR"),
                    getRandClient());
            fail("Expected bad request exception for deleted rewriter");
        } catch (final SolrException e) {
            assertEquals(SolrException.ErrorCode.BAD_REQUEST.code, e.code());
        }

    }

    @Test
    public void testGetConfig() throws IOException, SolrServerException {

        final String rewriterName = "conf_common_rules";
        final SolrClient client = getRandClient();
        final CommonRulesConfigRequestBuilder configBuilder = new CommonRulesConfigRequestBuilder();

        assertEquals(0, configBuilder.rules("a =>\n SYNONYM: b").ignoreCase(false).buildSaveRequest(rewriterName)
                .process(client).getStatus());

        final GetRewriterConfigSolrResponse response = buildGetRequest(rewriterName).process(client);
        assertEquals(0, response.getStatus());
        assertEquals(configBuilder.buildDescription(), response.getResponse().get(rewriterName));

    }

    @Test
    public void testGetAllConfig() throws IOException, SolrServerException {

        final String rewriterName = "conf_common_rules";
        final SolrClient client = getRandClient();
        final CommonRulesConfigRequestBuilder configBuilder = new CommonRulesConfigRequestBuilder();

        assertEquals(0, configBuilder.rules("a =>\n SYNONYM: b").ignoreCase(false).buildSaveRequest(rewriterName)
                .process(client).getStatus());

        final GetRewriterConfigSolrResponse response = buildGetRequest(null).process(client);
        assertEquals(0, response.getStatus());
        assertEquals(configBuilder.buildDescription(), ((HashMap<String, Object>) ((HashMap<String, Object>) response.getResponse().get("response")).get("rewriters")).get(rewriterName));

    }

    @Test
    public void testGetAllEmptyConfig() throws IOException, SolrServerException {

        final GetRewriterConfigSolrResponse response = buildGetRequest(null).process(getRandClient());
        assertEquals(0, response.getStatus());
        assertEquals(0, ((HashMap<String, Object>) ((HashMap<String, Object>) response.getResponse().get("response")).get("rewriters")).size());

    }

    @Test
    public void testFor404WhenDeletingUnknownRewriter() throws IOException, SolrServerException {
        try {
            buildDeleteRequest("delete_the_void")
                    .process(getRandClient());

            fail("Expected rewriter not found exception while deleting");
        } catch (final SolrException e) {
            assertEquals(SolrException.ErrorCode.NOT_FOUND.code, e.code());
        }
    }

    @Test
    public void testFor400WhenUsingUnknownRewriter() throws IOException, SolrServerException {

        try {
            new QueryRequest(
                    params("collection", COLLECTION,
                            "q", "a",
                            "defType", "querqy",
                            PARAM_REWRITERS, "delete_common_rules",
                            DisMaxParams.QF, "f1 f2",
                            QueryParsing.OP, "OR")).process(getRandClient());

            fail("Expected rewriter not found exception while querying");
        } catch (final SolrException e) {
            assertEquals(SolrException.ErrorCode.BAD_REQUEST.code, e.code());
        }
    }

    private SolrClient getRandClient() {
        return getRandClient(random(), CLIENTS, CLOUD_CLIENT);
    }

}
