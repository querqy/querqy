package querqy.solr;

import org.apache.lucene.util.TestUtil;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.cloud.AbstractDistribZkTestBase;
import org.apache.solr.cloud.SolrCloudTestCase;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;

public class AbstractQuerqySolrCloudTestCase extends SolrCloudTestCase {
    /**
     * returns a random SolrClient -- either a CloudSolrClient, or an HttpSolrClient pointed
     * at a node in our cluster
     */
    public static SolrClient getRandClient(final Random rand, final List<? extends SolrClient> clients,
                                           final SolrClient cloudClient) {
        int numClients = clients.size();
        int idx = TestUtil.nextInt(rand, 0, numClients);

        return (idx == numClients) ? cloudClient : clients.get(idx);
    }

    public static void waitForRecoveriesToFinish(final CloudSolrClient client) throws Exception {
        assert null != client.getDefaultCollection();
        AbstractDistribZkTestBase.waitForRecoveriesToFinish(client.getDefaultCollection(), client.getZkStateReader(),
                true, true, 330);
    }

    protected QueryResponse waitForRewriterAndQuery(final QueryRequest req, final SolrClient client) throws Exception {
        // It will take a bit to propagate a rewriter config to the nodes. We try to apply the rewriter max. 3 times and
        // wait for a bit between the attempts

        int attempts = 3;
        do {
            synchronized (this) {
                wait(800L);
            }
            try {
                return req.process(client);

            } catch (Exception e) {
                if ((attempts <= 1) || (!e.getMessage().contains("No such rewriter"))) {
                    throw e;
                }
                attempts--;
            }
        } while (attempts > 0);
        throw new TimeoutException("Rewriter didn't come up");
    }
}
