package querqy.solr;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;

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
import querqy.solr.RewriterConfigRequestBuilder.SaveRewriterConfigSolrResponse;
import querqy.solr.rewriter.replace.ReplaceConfigRequestBuilder;
import querqy.solr.rewriter.commonrules.CommonRulesConfigRequestBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SolrTestCaseJ4.SuppressSSL
public class QuerqyRewriterRequestHandlerSolrCloudTest extends AbstractQuerqySolrCloudTestCase {

    final static String COLLECTION = "basic1";

    /** A basic client for operations at the cloud level, default collection will be set */
    private static CloudSolrClient CLOUD_CLIENT;

    /** One client per node */
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

    @Test
    public void testSimpleRequest() throws IOException, SolrServerException {
        final SolrParams params = params("collection", COLLECTION, "q", "*:*", "rows", "10", "defType", "querqy");
        final QueryRequest request = new QueryRequest(params);
        QueryResponse rsp = request.process(getRandClient());
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
    public void testDeleteRewriter() throws Exception {

        assertEquals(0, new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: b").buildSaveRequest("delete_common_rules")
                .process(getRandClient())
                .getStatus());

        final SolrClient client = getRandClient();

        final QueryResponse rsp = waitForRewriterAndQuery(new QueryRequest(
                params("collection", COLLECTION,
                        "q", "a",
                        "defType", "querqy",
                        PARAM_REWRITERS, "delete_common_rules",
                        DisMaxParams.QF, "f1 f2",
                        QueryParsing.OP, "OR")

        ), client);


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

    private SolrClient getRandClient() {
        return getRandClient(random(), CLIENTS, CLOUD_CLIENT);
    }

}
