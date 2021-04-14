package querqy.solr;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.ZkRewriterContainer.IO_DATA;
import static querqy.solr.ZkRewriterContainer.IO_PATH;

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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.solr.rewriter.commonrules.CommonRulesConfigRequestBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SolrTestCaseJ4.SuppressSSL
public class ZkStoragePathTest extends AbstractQuerqySolrCloudTestCase {

    final static String COLLECTION = "storagepath";

    /** A basic client for operations at the cloud level, default collection will be set */
    private static CloudSolrClient CLOUD_CLIENT;

    /** One client per node */
    private static ArrayList<HttpSolrClient> CLIENTS = new ArrayList<>(5);

    private static SolrZkClient ZK_CLIENT;

    @BeforeClass
    public static void setupCluster() throws Exception {

        configureCluster(2)
                .addConfig("storagepath", getFile("solrcloud").toPath().resolve("configsets").resolve("storagepath")
                        .resolve("conf"))
                .configure();

        CollectionAdminRequest.createCollection(COLLECTION, "storagepath", 2, 1).process(cluster.getSolrClient());
        cluster.waitForActiveCollection(COLLECTION, 2, 2);

        CLOUD_CLIENT = cluster.getSolrClient();
        CLOUD_CLIENT.setDefaultCollection(COLLECTION);

        waitForRecoveriesToFinish(CLOUD_CLIENT);

        for (JettySolrRunner jetty : cluster.getJettySolrRunners()) {
            CLIENTS.add(getHttpSolrClient(jetty.getBaseUrl() + "/" + COLLECTION + "/"));
        }

        ZK_CLIENT = zkClient();
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
    public void testCustomConfigPathWithLargeConfigFile() throws Exception {
        String configuredConfigName = "incredible-querqy";

        // we upload a 2.3 MB rules.txt, which can be compressed to < 1 MB
        assertEquals(0, new CommonRulesConfigRequestBuilder()
                .rules(getClass().getClassLoader().getResourceAsStream("configs/commonrules/rules.txt"))
                .buildSaveRequest("some_common_rules")
                .process(getRandClient())
                .getStatus());

        // issue query and wait for rewriters to become available
        final QueryResponse rsp = waitForRewriterAndQuery(
            params("collection", COLLECTION,
                    "q", "c",
                    "defType", "querqy",
                    PARAM_REWRITERS, "some_common_rules",
                    DisMaxParams.QF, "f1 f2",
                    QueryParsing.OP, "OR"),
            getRandClient());
        assertNotNull(rsp);
        assertEquals(1L, rsp.getResults().getNumFound());

        final List<String> children = ZK_CLIENT.getChildren("/configs/" + configuredConfigName + "/" + IO_PATH + "/" + IO_DATA, null, true)
                .stream().filter(name -> name.contains("some_common_rules-")).collect(Collectors.toList());
        assertTrue(children.size() >= 1);
    }

    private SolrClient getRandClient() {
        return getRandClient(random(), CLIENTS);
    }

}
