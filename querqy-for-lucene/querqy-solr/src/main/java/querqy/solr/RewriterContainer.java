package querqy.solr;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CloseHook;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import querqy.rewrite.RewriterFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class RewriterContainer<R extends SolrResourceLoader> {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    protected Map<String, RewriterFactory> rewriters = new HashMap<>();
    protected R resourceLoader;
    protected SolrCore core;
    private RewritersChangeListener rewritersChangeListener = null;

    public interface RewritersChangeListener {
        void rewritersChanged(SolrIndexSearcher indexSearcher, Set<RewriterFactory> allRewriters);
    }

    protected RewriterContainer(final SolrCore core, final R resourceLoader) {
        if (core.getResourceLoader() != resourceLoader) {
            throw new IllegalArgumentException("ResourceLoader doesn't belong to this SolrCore");
        }
        this.core = core;
        this.resourceLoader = resourceLoader;
        this.core.addCloseHook(new CloseHook(){

            /**
             * (1) Is called before any component is closed. To guarantee consistency,
             * we keep the container alive as long as the QuerqyRewriterRequestHandler
             * is not closed
             */
            @Override
            public void preClose(SolrCore core) {
                // noop
            }

            /**
             * (2) the QuerqyRewriterRequestHandler is closed
             * 
             * (3) the SolrCore is closed
             * 
             * (4) We are going to close the RewriterContainer
             */
            @Override
            public void postClose(SolrCore core) {
                close();
            }
            
        });
    }

    protected abstract void init(@SuppressWarnings({"rawtypes"}) NamedList args);

    /**
     * Close hook that will be triggered on close.
     */
    protected abstract void doClose();

    protected abstract void doSaveRewriter(final String rewriterId, final Map<String, Object> instanceDescription)
            throws IOException;
    protected abstract void deleteRewriter(final String rewriterId) throws IOException;

    /**
     * The rewriter description is used for the rest endpoints to get detailed information to the rewriter chains.
     */
    public abstract Map<String, Object> readRewriterDefinition(String rewriterId) throws IOException;

    public void saveRewriter(final String rewriterId, final Map<String, Object> instanceDescription)
            throws IOException {

        validateRewriterDescription(rewriterId, instanceDescription);
        doSaveRewriter(rewriterId, instanceDescription);

    }

    public Optional<RewriterFactory> getRewriterFactory(final String rewriterId) {
        return Optional.ofNullable(rewriters.get(rewriterId));
    }

    public synchronized Collection<RewriterFactory> getRewriterFactories(final RewritersChangeListener listener) {
        this.rewritersChangeListener = listener;
        return rewriters.values();
    }

    public final synchronized void close() {
        doClose();
        rewritersChangeListener = null;
        resourceLoader = null;
        core = null;
        rewriters = null;
    }

    protected synchronized void loadRewriter(final String rewriterId, final Map<String, Object> instanceDesc) throws
            Exception {

        final SolrRewriterFactoryAdapter factoryLoader = SolrRewriterFactoryAdapter.loadInstance(rewriterId,
                instanceDesc);
        factoryLoader.configure((Map<String, Object>) instanceDesc.getOrDefault("config", Collections.emptyMap()));

        final Map<String, RewriterFactory> newRewriters = new HashMap<>(rewriters);
        newRewriters.put(rewriterId, factoryLoader.getRewriterFactory());
        rewriters = newRewriters;
        LOG.info("Loaded rewriter: {}", rewriterId);

    }

    protected synchronized void notifyRewritersChangeListener() {

        if (rewritersChangeListener != null && !rewriters.isEmpty()) {

            // We must not call lister.rewritersChanges() asynchronously. If we did, we might happen to decref and
            // possibly let the core close the searcher prematurely
            final RefCounted<SolrIndexSearcher> refCounted = core.getSearcher();
            try {
                rewritersChangeListener.rewritersChanged(refCounted.get(), new HashSet<>(rewriters.values()));
            } finally {
                refCounted.decref();
            }
        }
    }

    private void validateRewriterDescription(final String rewriterId, final Map<String, Object> instanceDescription) {

        final SolrRewriterFactoryAdapter factoryLoader = SolrRewriterFactoryAdapter.loadInstance(rewriterId,
                instanceDescription);
        final List<String> errors = factoryLoader.validateConfiguration(
                (Map<String, Object>) instanceDescription.getOrDefault("config", Collections.emptyMap()));
        if (errors != null && !errors.isEmpty()) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                    "Invalid configuration for rewriter " + rewriterId + " " + String.join("; ", errors));
        }

    }


}
