package querqy.solr.it;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.eclipse.jetty.client.HttpClient;
import org.testcontainers.containers.SolrClientUtils;

/**
 * Mostly copied code from {@link SolrClientUtils} as it's not open for
 * extension :-/
 */
public class QuerqySolrClientUtils extends SolrClientUtils {

    private static final String CHORUS_DATASET_URL = "https://querqy.org/datasets/icecat/icecat-products-w_price-19k-20201127.tar.gz";

    /**
     * 
     * @param hostname          the Hostname under which solr is reachable
     * @param port              The Port on which solr is running
     * @param collectionName    the name of the collection which should be created
     * @param configurationName the name of the configuration which should used to
     *                          create the collection or null if the default
     *                          configuration should be used
     * @param numShards         the number of shards in the new collection
     * @throws URISyntaxException
     * @throws IOException
     * 
     * @see SolrClientUtils
     */
    public static void createCollection(QuerqySolrContainer solr, String collectionName, String configurationName,
            int numShards) throws URISyntaxException, IOException {

        // compose create collection url
        HttpGet createCollection = new HttpGet(String.format(
                "%s/admin/collections?action=CREATE&name=%s&numShards=%s&replicationFactor=1&wt=json&collection.configName=%s&maxShardsPerNode=%s",
                solr.getSolrUrl(), collectionName, numShards, configurationName, numShards));

        // execute request
        try (CloseableHttpClient client = HttpClients.createMinimal();
            CloseableHttpResponse response = client.execute(createCollection);) {
            
            if (response.getStatusLine().getStatusCode() > 299) {
                throw new IllegalArgumentException(response.getStatusLine().getReasonPhrase());
            }

        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Imports the icecat chorus dataset into given collection
     */
    public static void importChorusDataset(QuerqySolrContainer solr, String collectionName) throws IOException {

        // read chorus data set
        String chorusDataSet = FileUtils.readFileToString(solr.getTestDataPath().toFile(), "UTF-8");

        // compose create collection url
        HttpPost importData = new HttpPost(String.format(
                "%s/%s/update?commit=true",
                solr.getSolrUrl(), collectionName));
        importData.setEntity(new StringEntity(chorusDataSet, ContentType.APPLICATION_JSON));

        // execute request
        try (CloseableHttpClient client = HttpClients.createMinimal();
            CloseableHttpResponse response = client.execute(importData);) {
            
            if (response.getStatusLine().getStatusCode() > 299) {
                throw new IllegalArgumentException(response.getStatusLine().getReasonPhrase());
            }

        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
