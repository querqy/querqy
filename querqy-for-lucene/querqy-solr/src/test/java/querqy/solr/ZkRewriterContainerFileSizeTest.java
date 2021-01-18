package querqy.solr;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.ZkRewriterContainer.IO_DATA;
import static querqy.solr.ZkRewriterContainer.IO_PATH;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.search.QueryParsing;
import org.apache.zookeeper.KeeperException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.solr.rewriter.commonrules.CommonRulesConfigRequestBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SolrTestCaseJ4.SuppressSSL
public class ZkRewriterContainerFileSizeTest extends AbstractQuerqySolrCloudTestCase {

    final static String COLLECTION = "fsize1";

    /** A basic client for operations at the cloud level, default collection will be set */
    private static CloudSolrClient CLOUD_CLIENT;

    /** One client per node */
    private static final ArrayList<HttpSolrClient> CLIENTS = new ArrayList<>(5);


    @BeforeClass
    public static void setupCluster() throws Exception {

        configureCluster(4)
                .addConfig("fsize", getFile("solrcloud").toPath().resolve("configsets").resolve("filesizetest")
                        .resolve("conf"))
                .configure();

        CollectionAdminRequest.createCollection(COLLECTION, "fsize", 2, 1).process(cluster.getSolrClient());
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
    public void testLargeConfig() throws Exception {

        // we upload a 2.3 MB rules.txt, which can be compressed to < 1 MB
        assertEquals(0, new CommonRulesConfigRequestBuilder()
                .rules(getClass().getClassLoader().getResourceAsStream("configs/commonrules/rules-large.txt"))
                .buildSaveRequest("large_common_rules")
                .process(getRandClient())
                .getStatus());

        final QueryResponse rsp = waitForRewriterAndQuery(
                params("collection", COLLECTION,
                        "q", "f",
                        "defType", "querqy",
                        PARAM_REWRITERS, "large_common_rules",
                        DisMaxParams.QF, "f1 f2",
                        QueryParsing.OP, "OR"),
                getRandClient());

        assertNotNull(rsp);
        assertEquals(1L, rsp.getResults().getNumFound());
        final SolrZkClient zkClient = zkClient();
        final List<String> children = zkClient.getChildren("/configs/fsize/" + IO_PATH + "/" + IO_DATA, null, true)
                .stream().filter(name -> name.contains("large_common_rules-")).collect(Collectors.toList());
        assertTrue(children.size() > 1);
        for (final String child : children) {
            final byte[] data = zkClient.getData("/configs/fsize/" + IO_PATH + "/" + IO_DATA + "/" + child, null, null,
                    true);
            assertTrue(data.length <= 1000); // 1000 is the max file size configured in solrconfig.xml
        }

    }

    @Test
    public void testNoFilesLeftAfterDelete() throws IOException, SolrServerException, KeeperException,
            InterruptedException {

        assertEquals(0, new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: b").buildSaveRequest("delete_common_rules")
                .process(getRandClient())
                .getStatus());

        final SolrZkClient zkClient = zkClient();
        List<String> children = zkClient.getChildren("/configs/fsize/" + IO_PATH + "/" + IO_DATA, null, true);
        assertTrue(children.stream().anyMatch(name -> name.contains("delete_common_rules-")));

        assertEquals(0, RewriterConfigRequestBuilder.buildDeleteRequest("delete_common_rules")
                .process(getRandClient())
                .getStatus());


        // test that the data files of the rewriter have been removed as well
        children = zkClient.getChildren("/configs/fsize/" + IO_PATH + "/" + IO_DATA, null, true);
        assertFalse(children.stream().anyMatch(name -> name.contains("delete_common_rules-")));
    }

    private SolrClient getRandClient() {
        return getRandClient(random(), CLIENTS, CLOUD_CLIENT);
    }

}
