package querqy.solr;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import querqy.lucene.rewrite.infologging.Sink;
import querqy.rewrite.RewriterFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.solr.common.SolrException.ErrorCode.NOT_FOUND;

/**
 * Store the rewriters only in memory. The Map which stores the rewriters is only used to return the rewriter descriptions.
 * Due to the design, the rewriters are stored in memory in the parent rewriters variable.
 */
public class InMemoryRewriteContainer extends RewriterContainer<SolrResourceLoader> {

    private final Map<String, Map<String, Object>> store = new ConcurrentHashMap<>();

    public InMemoryRewriteContainer(final SolrCore core, final SolrResourceLoader resourceLoader,
                                    final Map<String, Sink> infoLoggingSinks) {
        super(core, resourceLoader, infoLoggingSinks);
    }

    @Override
    protected void init(final NamedList args) {
        // nothing to do
    }

    @Override
    protected void doClose() {
    }

    @Override
    protected synchronized void deleteRewriter(final String rewriterId) {
        store.remove(rewriterId);

        final Map<String, RewriterFactoryContext> newRewriters = new HashMap<>(rewriters);
        if ((newRewriters.remove(rewriterId) == null) && !store.containsKey(rewriterId)) {
            throw new SolrException(NOT_FOUND, "No such rewriter: " + rewriterId);
        }
        rewriters = newRewriters;
    }

    @Override
    public synchronized Map<String, Object> readRewriterDefinition(String rewriterId) {
        return store.get(rewriterId);
    }

    @Override
    protected synchronized void doSaveRewriter(String rewriterId, Map instanceDescription) {
        // this is only for rewrite description...

        try {
            loadRewriter(rewriterId, instanceDescription);
        } catch (final Exception e) {
            // this shouldn't happen
            throw new RuntimeException(e);
        }
        store.put(rewriterId, instanceDescription);
    }
}
