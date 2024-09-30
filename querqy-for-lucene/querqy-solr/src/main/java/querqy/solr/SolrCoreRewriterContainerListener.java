package querqy.solr;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrEventListener;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Optional;

import static org.apache.solr.common.SolrException.ErrorCode.CONFLICT;
import static querqy.solr.utils.CoreUtils.*;

/**
 * Registers an event listener in the querqy configuration core that notifies querqy request handlers
 * about changes in the rewriter configuration core.
 *
 * @author Markus Schuch
 */
public class SolrCoreRewriterContainerListener extends SearchComponent implements SolrCoreAware {

    private boolean enabled;

    private String querqyComponentName;

    @Override
    public void init(NamedList args) {
        var initArgs = args.toSolrParams();
        this.enabled = initArgs.getBool("enabled", false);
        this.querqyComponentName = initArgs.get("querqyRequestHandlerName", "/querqy/rewriter");
    }

    @Override
    public void inform(SolrCore core) {
        if (core.getCoreContainer().isZooKeeperAware()) {
            throw new SolrException(CONFLICT, "SolrCoreRewriterContainerListener cannot be used in solr cloud mode");
        }

        if (this.enabled) {
            SolrCoreUpdateListener listener = new SolrCoreUpdateListener(this.querqyComponentName);
            core.registerNewSearcherListener(listener);
        }
    }

    @Override
    public void prepare(ResponseBuilder rb) {
        // nothing to do here
    }

    @Override
    public void process(ResponseBuilder rb) {
        // nothing to do here
    }

    @Override
    public String getDescription() {
        return this.getClass().getSimpleName();
    }

    public static class SolrCoreUpdateListener implements SolrEventListener {

        private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

        private final String querqyComponentName;

        public SolrCoreUpdateListener(String querqyComponentName) {
            this.querqyComponentName = querqyComponentName;
        }

        @Override
        public void postCommit() {
            // noop
        }

        @Override
        public void postSoftCommit() {
            // noop
        }

        @Override
        public void newSearcher(SolrIndexSearcher newSearcher, SolrIndexSearcher currentSearcher) {
            LOG.info("Querqy rewriter data was updated. Notifying all configured core using querqy...");

            var coreContainer = newSearcher.getCore().getCoreContainer();

            coreContainer.getAllCoreNames()
                    .parallelStream()
                    .forEach(coreName -> {
                try {
                    withCore(
                            core -> {
                                notifyRewriterConfigChanged(core, newSearcher);
                                return null;
                            },
                            coreName,
                            coreContainer,
                            Duration.ofMinutes(1)
                    );
                } catch (IOException e) {
                    LOG.error("Unable to notify querqy rewriter request handler in core {}", coreName, e);
                }
            });
        }

        private void notifyRewriterConfigChanged(SolrCore core, SolrIndexSearcher newSearcher) {
            Optional<SolrRequestHandler> requestHandler = Optional.ofNullable(core.getRequestHandler(this.querqyComponentName));
            requestHandler.ifPresent(it -> {
                    if (it instanceof QuerqyRewriterRequestHandler) {
                        ((QuerqyRewriterRequestHandler) it).notifyRewriterConfigChanged(newSearcher);
                    } else {
                        LOG.error("Unable to notify querqy rewriter request handler in core {}: unexpected handler type {}", core.getName(), it.getClass().getName());
                    }
                }
            );
        }

        @Override
        public void init(NamedList args) {
            // noop
        }
    }

}
