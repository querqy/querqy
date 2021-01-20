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
            params.put(PARAM_ACTION, new String[]{name()});
            this.params = new MultiMapSolrParams(params);
        }

        static Optional<ActionParam> fromRequest(final SolrQueryRequest req) {

            // Solr V1 API can only handle GET and POST.
            final String method = req.getHttpMethod();

            final String actionString = req.getParams().get(PARAM_ACTION);
            if (actionString == null && method == null) {
                return Optional.empty();
            }

            final Optional<ActionParam> actionParam = fromString(actionString);
            if (method == null) {
                return actionParam;
            }

            if (method.equalsIgnoreCase("POST")) {

                if (!actionParam.isPresent()) {
                    throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "HTTP POST requires parameter: "
                            + PARAM_ACTION);
                }

                switch (actionParam.get()) {
                    case SAVE:
                    case DELETE:
                        return actionParam;
                    default:
                        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "HTTP POST must not be combined " +
                                "with " + PARAM_ACTION + "=" + actionString);

                }

            } else if (method.equalsIgnoreCase("GET")) {

                if (!actionParam.isPresent()) {
                    return Optional.of(GET);
                }
                if (actionParam.get() == ActionParam.GET) {
                    return actionParam;
                }
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "HTTP GET must not be combined " +
                        "with " + PARAM_ACTION + "=" + actionString);

            } else if (method.equalsIgnoreCase("DELETE")) {

                if (!actionParam.isPresent()) {
                    return Optional.of(DELETE);
                }
                if (actionParam.get() == ActionParam.DELETE) {
                    return actionParam;
                }
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "HTTP DELETE must not be combined " +
                        "with " + PARAM_ACTION + "=" + actionString);

            } else if (method.equalsIgnoreCase("PUT")) {

                if (!actionParam.isPresent()) {
                    return Optional.of(SAVE);
                }
                if (actionParam.get() == ActionParam.SAVE) {
                    return actionParam;
                }
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "HTTP PUT must not be combined " +
                        "with " + PARAM_ACTION + "=" + actionString);

            } else {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Unknown HTTP Method: " + method);
            }

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

    private RewriterContainer<?> rewriterContainer = null;

    @SuppressWarnings({"rawtypes"})
    private NamedList initArgs = null;

    @Override
    public void init(final NamedList args) {
        this.initArgs = args;
    }

    @Override
    public void handleRequest(final SolrQueryRequest req, final SolrQueryResponse rsp) {
        final Map<String, RewriterFactory> rewriters = rewriterContainer.rewriters;
        try {
            final Map<String, Object> result = new HashMap<>();
            final Map<String, Map<String, Object>> rewritersResult = new HashMap<>();
            for (String rewriterId : rewriters.keySet()) {
                rewritersResult.put(rewriterId, rewriterContainer.readRewriterDescription(rewriterId));
            }
            result.put("rewriters", rewritersResult);
            rsp.add("response", result);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
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
        Boolean inMemory = (Boolean) initArgs.get("inMemory");
        if (inMemory != null && inMemory) {
            rewriterContainer = new InMemoryRewriteContainer(core, resourceLoader);
        } else if (resourceLoader instanceof ZkSolrResourceLoader) {
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


                final ActionParam action = fromRequest(req).orElse(GET);

                try {
                    switch (action) {
                        case SAVE:
                            doPut(req, rewriterId);
                            break;
                        case DELETE:
                            rewriterContainer.deleteRewriter(rewriterId);
                            break;
                        case GET:
                            rsp.add(rewriterId, rewriterContainer.readRewriterDescription(rewriterId));
                            break;
                    }
                } catch (final IOException e) {
                    throw new RuntimeException(e);
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

    public void doPut(final SolrQueryRequest req, final String rewriterId) throws IOException {

        final Iterable<ContentStream> streams = req.getContentStreams();
        final Iterator<ContentStream> iterator = streams.iterator();
        if (!iterator.hasNext()) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Empty request");
        }

        rewriterContainer.saveRewriter(rewriterId, readJson(iterator.next().getStream(), Map.class));
    }
}
