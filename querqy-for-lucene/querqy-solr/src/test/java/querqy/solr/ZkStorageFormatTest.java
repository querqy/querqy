package querqy.solr;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.ZkRewriterContainer.IO_DATA;
import static querqy.solr.ZkRewriterContainer.IO_PATH;

import org.apache.commons.io.IOUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.search.QueryParsing;
import org.apache.zookeeper.CreateMode;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.solr.rewriter.commonrules.CommonRulesConfigRequestBuilder;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;

@SolrTestCaseJ4.SuppressSSL
public class ZkStorageFormatTest extends AbstractQuerqySolrCloudTestCase {

    final static String COLLECTION = "storageformat";
    final static String CONFIGURED_CONFIG_NAME = "querqy-test";

    /** A basic client for operations at the cloud level, default collection will be set */
    private static CloudSolrClient CLOUD_CLIENT;

    /** One client per node */
    private static ArrayList<HttpSolrClient> CLIENTS = new ArrayList<>(5);

    private static SolrZkClient ZK_CLIENT;

    @BeforeClass
    public static void setupCluster() throws Exception {



        configureCluster(2)
                .addConfig("storageformat", getFile("solrcloud").toPath().resolve("configsets").resolve("storageformat")
                        .resolve("conf"))
                .configure();

        ZK_CLIENT = zkClient();

        // upload v1 config
        final byte[] inventory;
        final byte[] config;

        try (final FileInputStream fis = new FileInputStream(getFile("zkstorage").toPath().resolve("v1")
                .resolve("some_common_rules").toFile())) {
            inventory = IOUtils.toByteArray(fis);
        }

        try (final FileInputStream fis = new FileInputStream(getFile("zkstorage").toPath().resolve("v1")
                .resolve("some_common_rules-ed6e240a-e7e8-47b0-995a-b700a5f8c16d").toFile())) {
            config = IOUtils.toByteArray(fis);
        }


        ZK_CLIENT.makePath("/configs/" + CONFIGURED_CONFIG_NAME + "/" + IO_PATH + "/" + IO_DATA, true);

        ZK_CLIENT.create("/configs/" + CONFIGURED_CONFIG_NAME + "/" + IO_PATH + "/" + IO_DATA +
                "/some_common_rules-ed6e240a-e7e8-47b0-995a-b700a5f8c16d", config, CreateMode.PERSISTENT, true);
        ZK_CLIENT.create("/configs/" + CONFIGURED_CONFIG_NAME + "/" + IO_PATH + "/some_common_rules", inventory,
                CreateMode.PERSISTENT, true);


        CollectionAdminRequest.createCollection(COLLECTION, "storageformat", 2, 1).process(cluster.getSolrClient());
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
        try {
            ZK_CLIENT.close();
        } finally {
            ZK_CLIENT = null;
        }
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
    public void testThatRewriterInV1FormatCanBeReadAndReplacedWithNewConfig() throws Exception {

        // issue query and wait for rewriters to become available
        final QueryResponse rsp = waitForRewriterAndQuery(
                params("collection", COLLECTION,
                        "q", "a",
                        "defType", "querqy",
                        PARAM_REWRITERS, "some_common_rules",
                        DisMaxParams.QF, "f1 f2",
                        QueryParsing.OP, "OR"),
                getRandClient());
        assertNotNull(rsp);
        assertEquals(0L, rsp.getResults().getNumFound());

        // updating rules
        assertEquals(0, new CommonRulesConfigRequestBuilder()
                .rules("a =>\nSYNONYM:c")
                .buildSaveRequest("some_common_rules")
                .process(getRandClient())
                .getStatus());

        final QueryResponse rsp2 = waitForRewriterAndQuery(
                params("collection", COLLECTION,
                        "q", "a",
                        "defType", "querqy",
                        PARAM_REWRITERS, "some_common_rules",
                        DisMaxParams.QF, "f1 f2",
                        QueryParsing.OP, "OR"),
                getRandClient());
        assertNotNull(rsp2);
        assertEquals(2L, rsp2.getResults().getNumFound());

        // the old config must be gone now
        assertFalse(ZK_CLIENT.exists("/configs/" + CONFIGURED_CONFIG_NAME + "/" + IO_PATH + "/" + IO_DATA +
                "/some_common_rules-ed6e240a-e7e8-47b0-995a-b700a5f8c16d", true));


    }

    private SolrClient getRandClient() {
        return getRandClient(random(), CLIENTS);
    }


}
