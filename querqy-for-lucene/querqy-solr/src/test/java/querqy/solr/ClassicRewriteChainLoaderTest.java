package querqy.solr;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import querqy.solr.RewriterConfigRequestBuilder.GetRewriterConfigSolrResponse;

import java.io.IOException;
import java.util.Map;


public class ClassicRewriteChainLoaderTest extends AbstractQuerqySolrCloudTestCase {

    private static final String COLLECTION = "classic1";

    private static CloudSolrClient CLIENT;

    @BeforeClass
    public static void setupCluster() throws Exception {

        configureCluster(4)
                .addConfig("classic",
                        getFile("solrcloud").toPath().resolve("configsets").resolve("classic").resolve("conf"))
                .configure();

        CollectionAdminRequest.createCollection(COLLECTION, "classic", 2, 1).process(cluster.getSolrClient());
        cluster.waitForActiveCollection(COLLECTION, 2, 2);

        CLIENT = cluster.getSolrClient();
        CLIENT.setDefaultCollection(COLLECTION);

        waitForRecoveriesToFinish(CLIENT);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (CLIENT != null) {
            CLIENT.close();
            CLIENT = null;
        }
    }

    @Test
    public void shouldUploadClassicRewriteChain() throws SolrServerException, IOException {
        GetRewriterConfigSolrResponse response = RewriterConfigRequestBuilder.buildGetRequest(null).process(CLIENT);

        // this is quite ugly class casting :-/
        // rewriters
        Map<String, Object> rewriters = (Map<String, Object>) ((java.util.Map<String, Object>) response.getResponse()
                .get("response")).get("rewriters");
        org.hamcrest.MatcherAssert.assertThat(rewriters, not(nullValue()));
        org.hamcrest.MatcherAssert.assertThat(rewriters, Matchers.aMapWithSize(1));

        // classic rewriter config
        Map<String, Object> classic = (Map<String, Object>) rewriters.get("classic");
        org.hamcrest.MatcherAssert.assertThat(classic, not(nullValue()));
        org.hamcrest.MatcherAssert.assertThat(classic, Matchers.aMapWithSize(2));
        org.hamcrest.MatcherAssert.assertThat(classic.get("id"), is("classic"));
        org.hamcrest.MatcherAssert.assertThat(classic.get("path"), is("/querqy/rewriter/classic"));
    }
}
