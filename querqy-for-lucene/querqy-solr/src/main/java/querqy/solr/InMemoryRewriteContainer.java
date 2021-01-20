package querqy.solr;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import querqy.rewrite.RewriterFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.solr.common.SolrException.ErrorCode.NOT_FOUND;

public class InMemoryRewriteContainer extends RewriterContainer<SolrResourceLoader> {

    private final Map<String, Map<String, Object>> store = new ConcurrentHashMap<>();

    public InMemoryRewriteContainer(SolrCore core, SolrResourceLoader resourceLoader) {
        super(core, resourceLoader);
    }

    @Override
    protected void init(NamedList args) {
        // nothing to do
    }

    @Override
    protected synchronized void deleteRewriter(String rewriterId) {
        store.remove(rewriterId);

        final Map<String, RewriterFactory> newRewriters = new HashMap<>(rewriters);
        if ((newRewriters.remove(rewriterId) == null) && !store.containsKey(rewriterId)) {
            throw new SolrException(NOT_FOUND, "No such rewriter: " + rewriterId);
        }
        rewriters = newRewriters;

        notifyRewritersChangeListener();
    }

    @Override
    public synchronized Map<String, Object> readRewriterDescription(String rewriterId) {
        return store.get(rewriterId);
    }

    @Override
    protected synchronized void doSaveRewriter(String rewriterId, Map instanceDescription) {
        // this is for rewrite description...
        store.put(rewriterId, instanceDescription);

        loadRewriter(rewriterId, instanceDescription);
        notifyRewritersChangeListener();
    }
}
