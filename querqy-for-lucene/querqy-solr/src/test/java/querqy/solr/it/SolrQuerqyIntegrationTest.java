package querqy.solr.it;

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.utility.DockerImageName;

@RunWith(Parameterized.class)
@Category(IntegrationTest.class)
public class SolrQuerqyIntegrationTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Parameters
    public static Iterable<? extends Object> solrVersionsToTest() {
        return QuerqySolrContainer.getSolrTestVersions();
    }

    @Rule
    public QuerqySolrContainer solr;

    public SolrQuerqyIntegrationTest(DockerImageName solrImage) {
        this.solr = new QuerqySolrContainer(solrImage);
    }

    @Test
    public void testContainerUpAndRunning() {
        log.info(String.format("Testing Querqy in Solr %s", solr.getDockerImageName()));
        assertTrue(true);
    }
}
