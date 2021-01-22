package querqy.solr;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import querqy.solr.RewriterConfigRequestBuilder.GetRewriterConfigSolrResponse;

public class ClassicRewriteChainLoaderTest extends AbstractQuerqySolrCloudTestCase {

    private static final String COLLECTION = "classic1";

    private static CloudSolrClient client;

    @BeforeClass
    public static void setupCluster() throws Exception {

        configureCluster(4)
                .addConfig("classic",
                        getFile("solrcloud").toPath().resolve("configsets").resolve("classic").resolve("conf"))
                .configure();

        CollectionAdminRequest.createCollection(COLLECTION, "classic", 2, 1).process(cluster.getSolrClient());
        cluster.waitForActiveCollection(COLLECTION, 2, 2);

        client = cluster.getSolrClient();
        client.setDefaultCollection(COLLECTION);

        waitForRecoveriesToFinish(client);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    @Test
    public void shouldUploadClassicRewriteChain() throws SolrServerException, IOException {
        GetRewriterConfigSolrResponse response = RewriterConfigRequestBuilder.buildGetRequest(null).process(client);

        // this is quite ugly class casting :-/
        // rewriters
        Map<String, Object> rewriters = (Map<String, Object>) ((java.util.Map<String, Object>) response.getResponse()
                .get("response")).get("rewriters");
        assertThat(rewriters, not(nullValue()));
        assertThat(rewriters, Matchers.aMapWithSize(1));

        // classic rewriter config
        Map<String, Object> classic = (Map<String, Object>) rewriters.get("classic");
        assertThat(classic, not(nullValue()));
        assertThat(classic, Matchers.aMapWithSize(4));
        assertThat(classic.get("querqyParser"), is("querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory"));
        assertThat(classic.get("ignoreCase"), is(Boolean.TRUE));
        assertThat(classic.get("class"), is("querqy.solr.rewriter.commonrules.CommonRulesRewriterFactory"));
        assertThat(classic.get("config"), not(nullValue()));
    }
}
