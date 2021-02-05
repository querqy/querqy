package querqy.solr;

import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.RewriterConfigRequestBuilder.buildDeleteRequest;
import static querqy.solr.RewriterConfigRequestBuilder.buildGetRequest;
import static querqy.solr.RewriterConfigRequestBuilder.buildListRequest;

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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.solr.RewriterConfigRequestBuilder.GetRewriterConfigSolrResponse;
import querqy.solr.RewriterConfigRequestBuilder.SaveRewriterConfigSolrResponse;
import querqy.solr.rewriter.replace.ReplaceConfigRequestBuilder;
import querqy.solr.rewriter.commonrules.CommonRulesConfigRequestBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
    private static List<HttpSolrClient> CLIENTS = new ArrayList<>(5);


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
        CLIENTS = null;
    }

    @Before
    public void setUp() throws Exception {

        super.setUp();

        final SolrClient randClient = getRandClient();
        randClient.deleteByQuery("*:*");
        randClient.commit(true, true);

        randClient.add(Arrays.asList(
                sdoc("id", "1", "f1", "a"),
                sdoc("id", "2", "f2", "b"),
                sdoc("id", "3", "f2", "c")
        ));

        randClient.commit(true, true);

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

        try {

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

        } finally {
            cleanUpRewriters("rewriter_test_save");
        }

    }

    @Test
    public void testRewriteChain() throws Exception {

        try {
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
        } finally {
            cleanUpRewriters("chain_replace", "chain_common_rules");
        }

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

        final QueryRequest request = new QueryRequest(params("collection", COLLECTION,
                "q", "a",
                "defType", "querqy",
                PARAM_REWRITERS, "delete_common_rules",
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR"));

        for (final SolrClient solrClient : CLIENTS) {

            int attempts = 20;

            while (attempts > 0) {

                try {
                    if (request.process(solrClient).getStatus() == 0) {
                        if (attempts == 1) {
                            fail("Expected bad request exception for deleted rewriter");
                        }
                        synchronized (this) {
                            wait(100L);
                        }
                        attempts--;
                    }

                } catch (final SolrException e) {
                    assertEquals(SolrException.ErrorCode.BAD_REQUEST.code, e.code());
                    attempts = -1;
                }

            }

            if (attempts == 0) {
                fail("Expected bad request exception for deleted rewriter");
            }
        }

    }

    @Test
    public void testGetConfig() throws Exception {

        try {

            final String rewriterName = "conf_common_rules";
            final SolrClient client = getRandClient();
            final CommonRulesConfigRequestBuilder configBuilder = new CommonRulesConfigRequestBuilder();

            assertEquals(0, configBuilder.rules("a =>\n SYNONYM: b").ignoreCase(false).buildSaveRequest(rewriterName)
                    .process(client).getStatus());

            final GetRewriterConfigSolrResponse response = buildGetRequest(rewriterName).process(client);
            assertEquals(0, response.getStatus());
            final Map<String, Object> conf = (Map<String, Object>) response.getResponse().get("rewriter");
            assertNotNull(conf);
            org.hamcrest.MatcherAssert.assertThat(conf, hasEntry("id", rewriterName));
            org.hamcrest.MatcherAssert.assertThat(conf, hasEntry("path",
                    QuerqyRewriterRequestHandler.DEFAULT_HANDLER_NAME + "/" + rewriterName));

            assertEquals(configBuilder.buildDefinition(), conf.get("definition"));

        } finally {
            cleanUpRewriters("conf_common_rules");
        }

    }

    @Test
    public void testListConfigs() throws Exception {

        final String rewriterName1 = "rewriter1";
        final String rewriterName2 = "rewriter2";

        try {
            final SolrClient client = getRandClient();
            final CommonRulesConfigRequestBuilder commonRulesConfigBuilder = new CommonRulesConfigRequestBuilder();

            assertEquals(0, commonRulesConfigBuilder.rules("a =>\n SYNONYM: b").ignoreCase(false).buildSaveRequest(rewriterName1)
                    .process(client).getStatus());

            final ReplaceConfigRequestBuilder replaceConfigBuilder = new ReplaceConfigRequestBuilder();
            assertEquals(0, replaceConfigBuilder
                    .rules("sd => a").buildSaveRequest(rewriterName2).process(getRandClient())
                    .getStatus());

            final Map<String, Object> rewriters = waitFor(buildListRequest(), client, response -> {
                assertEquals(0, response.getStatus());
                return (Map<String, Object>) ((Map<String, Object>) response.getResponse()
                        .get("response")).get("rewriters");
            });

            final Map<String, Object> conf1 = (Map<String, Object>) rewriters.get(rewriterName1);
            assertNotNull(conf1);
            org.hamcrest.MatcherAssert.assertThat(conf1, hasEntry("id", rewriterName1));
            org.hamcrest.MatcherAssert.assertThat(conf1, hasEntry("path",
                    QuerqyRewriterRequestHandler.DEFAULT_HANDLER_NAME + "/" + rewriterName1));

            final Map<String, Object> conf2 = (Map<String, Object>) rewriters.get(rewriterName2);
            assertNotNull(conf2);
            org.hamcrest.MatcherAssert.assertThat(conf2, hasEntry("id", rewriterName2));
            org.hamcrest.MatcherAssert.assertThat(conf2, hasEntry("path",
                    QuerqyRewriterRequestHandler.DEFAULT_HANDLER_NAME + "/" + rewriterName2));

        } finally {
            cleanUpRewriters(rewriterName1, rewriterName2);
        }
    }

    @Test
    public void testListAllEmptyConfig() throws Exception {

        final Map<String, Object> rewriters = waitFor(buildGetRequest(null), getRandClient(), response -> {
            assertEquals(0, response.getStatus());
            return (Map<String, Object>) ((Map<String, Object>) response.getResponse()
                    .get("response")).get("rewriters");
        });

        assertEquals(0, rewriters.size());

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

    @Test
    public void testFor400WhenGettingUnknownRewriter() throws IOException, SolrServerException {

        try {

            buildGetRequest("thisrewriterdoesnotexist").process(getRandClient());
            fail("404 expected");
        } catch (final SolrException e) {
            assertEquals(SolrException.ErrorCode.NOT_FOUND.code, e.code());
        }

    }

    @Test
    public void testGetAllConfig() throws Exception {

        final String rewriterName = "conf_common_rules";
        try {
            final SolrClient client = getRandClient();
            final CommonRulesConfigRequestBuilder configBuilder = new CommonRulesConfigRequestBuilder();

            assertEquals(0, configBuilder.rules("a =>\n SYNONYM: b").ignoreCase(false).buildSaveRequest(rewriterName)
                    .process(client).getStatus());

            //final ListRewriterConfigsSolrResponse response = .process(client);

            Map<String, Object> expectedResult = new HashMap<>();
            expectedResult.put("id", rewriterName);
            expectedResult.put("path", "/querqy/rewriter/" + rewriterName);

            final Map<String, Object> confResult = waitFor(buildListRequest(), client, response -> {
                assertEquals(0, response.getStatus());
                return (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) response.getResponse()
                        .get("response")).get("rewriters")).get(rewriterName);
            });

            assertEquals(expectedResult, confResult);

        } finally {
            cleanUpRewriters(rewriterName);
        }



    }

    @Test
    public void testGetAllEmptyConfig() throws Exception {

        final Map<String, Object> rewriters = waitFor(buildListRequest(), getRandClient(), response -> {
            assertEquals(0, response.getStatus());
            return (Map<String, Object>) ((Map<String, Object>) response.getResponse()
                    .get("response")).get("rewriters");
        });

        assertEquals(0, rewriters.size());

    }

    private void cleanUpRewriters(final String... rewriterId) throws Exception {

        Exception exception = null;

        for (final String rewriter : rewriterId) {
            try {
                buildDeleteRequest(rewriter).process(CLOUD_CLIENT);
            } catch (Exception e) {
                exception = e;
            }
        }

        if (exception != null) {
            throw exception;
        }

        int attempts = 20;

        // wait until rewriters disappeared from all clients
        for (final SolrClient client: CLIENTS) {

            for (final String rewriter : rewriterId) {
                boolean found = false;
                while (attempts > 0 && (found = buildListRequest().process(client).getRewritersConfigMap()
                        .containsKey(rewriter))) {
                    synchronized (this) {
                        wait(100L);
                    }
                    attempts--;
                }

                if (found) {
                    throw new IllegalStateException("Rewriter still visible: " + rewriter);
                }
            }

        }

    }

    private SolrClient getRandClient() {
        return getRandClient(random(), CLIENTS);
    }

}
