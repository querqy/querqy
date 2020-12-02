package querqy.solr;

import static querqy.solr.QuerqyRewriterRequestHandler.ActionParam.*;
import static querqy.solr.utils.JsonUtil.readJson;

import org.apache.solr.cloud.ZkSolrResourceLoader;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.MultiMapSolrParams;
import org.apache.solr.common.params.SolrParams;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public class QuerqyRewriterRequestHandler implements SolrRequestHandler, NestedRequestHandler, SolrCoreAware {

    public static final String PARAM_ACTION = "action";

    public enum ActionParam {

        SAVE, DELETE, GET;

        private final SolrParams params;

        ActionParam() {
            final Map<String, String[]> params = new HashMap<>(1);
            params.put(PARAM_ACTION, new String[] {name()});
            this.params = new MultiMapSolrParams(params);
        }

        static Optional<ActionParam> fromString(final String str) {
            if (str == null) {
                return Optional.empty();
            }
            if (SAVE.name().equalsIgnoreCase(str)) {
                return Optional.of(SAVE);
            }
            if (DELETE.name().equalsIgnoreCase(str)) {
                return Optional.of(DELETE);
            }
            if (GET.name().equalsIgnoreCase(str)) {
                return Optional.of(GET);
            }
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                    "Unknown action value: " + str);
        }

        public SolrParams params() {
            return params;
        }
    }

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

                final String rewriterId = subPath.charAt(0) == '/' ? subPath.substring(1) : subPath;

                if (rewriterId.indexOf('/') > 0 || rewriterId.isEmpty()) {
                    throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                            "Illegal rewriter ID: " + rewriterId);
                }

                // Solr V1 API can only handle GET and POST.
                final boolean isPost = req.getHttpMethod().equalsIgnoreCase("POST");
                final ActionParam action = fromString(req.getParams().get(PARAM_ACTION)).orElse(GET);

                switch (action) {
                    case SAVE:
                        if (!isPost) {
                            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                                    SAVE.name() + " only allowed with Http POST method");
                        }
                        doPut(req, rewriterId);
                        break;
                    case DELETE:
                        if (!isPost) {
                            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                                    DELETE.name() + " only allowed with Http POST method");
                        }
                        try {
                            rewriterContainer.deleteRewriter(rewriterId);
                        } catch (final IOException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case GET:
                        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                            GET.name() + " not implemented yet");

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
