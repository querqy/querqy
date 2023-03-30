package querqy.solr;

import org.apache.lucene.tests.util.TestUtil;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.cloud.AbstractDistribZkTestBase;
import org.apache.solr.cloud.SolrCloudTestCase;
import org.apache.solr.common.params.SolrParams;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class AbstractQuerqySolrCloudTestCase extends SolrCloudTestCase {
    /**
     * returns a random SolrClient
     */
    public static SolrClient getRandClient(final Random rand, final List<? extends SolrClient> clients) {

        return clients.get(TestUtil.nextInt(rand, 0, clients.size() - 1));
    }

    public static void waitForRecoveriesToFinish(final CloudSolrClient client) throws Exception {
        assert null != client.getDefaultCollection();
        AbstractDistribZkTestBase.waitForRecoveriesToFinish(client.getDefaultCollection(), cluster.getZkStateReader(),
                true, true, 330);
    }

    protected QueryResponse waitForRewriterAndQuery(final SolrParams params, final SolrClient client) throws Exception {
        return waitForRewriterAndQuery(new QueryRequest(params), client);
    }

    protected QueryResponse waitForRewriterAndQuery(final QueryRequest req, final SolrClient client) throws Exception {
        // It will take a bit to propagate a rewriter config to the nodes. We try to apply the rewriter max. 'attempts'
        // times and wait for a bit between the attempts

        int attempts = 50;
        do {
            synchronized (this) {
                wait(100L);
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

    protected <T, R extends SolrResponse> T waitFor(final SolrRequest<R> req, final SolrClient client,
                                                    final Function<R,T> extractor) throws Exception {
        // It will take a bit to propagate a rewriter config to the nodes. We try to apply the extractor max. 3 times
        // and wait for a bit between the attempts

        int attempts = 20;
        do {
            synchronized (this) {
                wait(100L);
            }

            try {
                final T result = extractor.apply(req.process(client));
                if (result != null) {
                    return result;
                }

            } catch (final Exception e) {
                if ((attempts <= 1)) {
                    throw e;
                }
                attempts--;
            }
        } while (attempts > 0);
        throw new TimeoutException("Expected object couldn't be found");
    }
}
