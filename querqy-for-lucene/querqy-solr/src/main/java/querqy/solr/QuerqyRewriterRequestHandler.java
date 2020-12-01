package querqy.solr;

import static querqy.solr.utils.JsonUtil.readJson;

import org.apache.solr.cloud.ZkSolrResourceLoader;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.handler.NestedRequestHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import querqy.rewrite.RewriterFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class QuerqyRewriterRequestHandler implements SolrRequestHandler, NestedRequestHandler, SolrCoreAware {

    public static final String DEFAULT_HANDLER_NAME = "/querqy/rewriter";

    private static final Logger LOG = LoggerFactory.getLogger(QuerqyRewriterRequestHandler.class);


    private RewriterContainer<?> rewriterContainer = null;

    @SuppressWarnings({"rawtypes"})
    private NamedList initArgs = null;

    @Override
    public void init(final NamedList args) {
        this.initArgs = args;
    }

    @Override
    public void handleRequest(final SolrQueryRequest req, final SolrQueryResponse rsp) {

    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public String getDescription() {
        return "Loads Querqy Rewriter Configs";
    }

    @Override
    public Category getCategory() {
        return Category.OTHER;
    }

    @Override
    public void inform(final SolrCore core) {

        final SolrResourceLoader resourceLoader = core.getResourceLoader();

        if (resourceLoader instanceof ZkSolrResourceLoader) {
            rewriterContainer = new ZkRewriterContainer(core, (ZkSolrResourceLoader) resourceLoader);

        } else {
            rewriterContainer = new StandAloneRewriterContainer(core, resourceLoader);
        }
        rewriterContainer.init(initArgs);


    }

    public Optional<RewriterFactory> getRewriterFactory(final String rewriterId) {
        return rewriterContainer.getRewriterFactory(rewriterId);
    }

    public synchronized Collection<RewriterFactory> getRewriterFactories(final RewriterContainer.RewritersChangeListener listener) {
        return rewriterContainer.getRewriterFactories(listener);
    }




    @Override
    public SolrRequestHandler getSubHandler(final String subPath) {
        return new SolrRequestHandler() {
            @Override
            public void init(final NamedList args) {

            }

            @Override
            public void handleRequest(final SolrQueryRequest req, final SolrQueryResponse rsp) {

                final String[] pathParts = subPath.split("/");
                if (pathParts.length < 2) {
                    throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                            "Expected rewriter name and action in path: " + subPath);
                }
                final boolean isPost = req.getHttpMethod().equalsIgnoreCase("POST");
                final String rewriterId = pathParts[pathParts.length - 2];
                switch (pathParts[pathParts.length - 1].toLowerCase(Locale.ROOT)) {
                    case "_delete":
                        if (!isPost) {
                            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                                    "_delete only allowed with Http POST method");
                        }
                        try {
                            rewriterContainer.deleteRewriter(rewriterId);
                        } catch (final IOException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case "_put":
                        if (!isPost) {
                            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                                    "_put only allowed with Http POST method");
                        }
                        doPut(req, rewriterId);
                        break;
                    case "_get": break;
                    default: throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                            "Expected rewriter name and action in path: " + subPath);
                }

            }

            @Override
            public String getName() {
                return QuerqyRewriterRequestHandler.class.getName() + "/" + subPath;
            }

            @Override
            public String getDescription() {
                return "Request handler to manage Querqy rewriter: " + subPath;
            }

            @Override
            public Category getCategory() {
                return Category.OTHER;
            }

        };

    }

    public void doPut(final SolrQueryRequest req, final String rewriterId) {

        final Iterable<ContentStream> streams = req.getContentStreams();
        final Iterator<ContentStream> iterator = streams.iterator();
        if (!iterator.hasNext()) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Empty request");
        }

        try {
            rewriterContainer.saveRewriter(rewriterId, readJson(iterator.next().getStream(), Map.class));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
