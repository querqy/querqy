package querqy.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class InvalidClassicRewriteChainConfigTest extends SolrTestCaseJ4  {

    @Test
    public void testThatInvalidConfigIsNotLoaded() throws Exception {
        initCore("solrconfig-invalid-classic-config.xml", "schema.xml");
        final QuerqyRewriterRequestHandler requestHandler = (QuerqyRewriterRequestHandler) h.getCore()
                .getRequestHandler(QuerqyRewriterRequestHandler.DEFAULT_HANDLER_NAME);
        assertTrue(requestHandler.isPersistingRewriters());

        // wait for the searcher to be warmed up
        RefCounted<SolrIndexSearcher> refCounted = null;
        for (int i = 0; i < 10; i++) {
            refCounted = h.getCore().getRegisteredSearcher();
            if (refCounted == null) {
                synchronized (this) {
                    wait(100L);
                }
            } else {
                break;
            }
        }
        if (refCounted == null) {
            throw new IllegalStateException("Couldn't get a searcher reference");
        }
        try {
            // "shingle" is the rewriter that what we tried to load from solrconfig.xml
            assertFalse(requestHandler.getRewriterFactory("shingle").isPresent());
        } finally {
            refCounted.decref();
        }
    }
}
