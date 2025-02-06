package querqy.solr;

import static java.util.stream.Collectors.toMap;
import static querqy.solr.QuerqyRewriterRequestHandler.ActionParam.*;
import static querqy.solr.QuerqyRewriterRequestHandler.SolrMode.*;
import static querqy.solr.QuerqyRewriterRequestHandler.RewriterStorageType.*;
import static querqy.solr.utils.JsonUtil.readJson;

import org.apache.solr.cloud.ZkSolrResourceLoader;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.MultiMapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.handler.NestedRequestHandler;
import org.apache.solr.metrics.SolrMetricsContext;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import querqy.lucene.rewrite.infologging.Sink;
import querqy.solr.explain.ExplainRewriteChainRequestHandler;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.*;

public class QuerqyRewriterRequestHandler implements SolrRequestHandler, NestedRequestHandler, SolrCoreAware {

    public static final String PARAM_ACTION = "action";
    public static final String PATH_EXPLAIN_CHAIN = "/_explain/chain";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void initializeMetrics(final SolrMetricsContext parentContext, final String scope) {
    }

    @Override
    public SolrMetricsContext getSolrMetricsContext() {
        return null;
    }


    public enum ActionParam {

        SAVE, DELETE, GET;

        private final SolrParams params;

        ActionParam() {
            final Map<String, String[]> params = new HashMap<>(1);
            params.put(PARAM_ACTION, new String[] {name()});
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

                if (actionParam.isEmpty()) {
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

                if (actionParam.isEmpty()) {
                    return Optional.of(GET);
                }
                if (actionParam.get() == ActionParam.GET) {
                    return actionParam;
                }
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "HTTP GET must not be combined " +
                        "with " + PARAM_ACTION + "=" + actionString);

            } else if (method.equalsIgnoreCase("DELETE")) {

                if (actionParam.isEmpty()) {
                    return Optional.of(DELETE);
                }
                if (actionParam.get() == ActionParam.DELETE) {
                    return actionParam;
                }
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "HTTP DELETE must not be combined " +
                        "with " + PARAM_ACTION + "=" + actionString);

            } else if (method.equalsIgnoreCase("PUT")) {

                if (actionParam.isEmpty()) {
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

            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Unknown action value: " + str);
        }

        public SolrParams params() {
            return params;
        }
    }

    public static final String DEFAULT_HANDLER_NAME = "/querqy/rewriter";

    private RewriterStorageType rewriterStorageType = null;

    private RewriterContainer<?> rewriterContainer = null;

    @SuppressWarnings({"rawtypes"})
    private NamedList initArgs = null;

    private final ExplainRewriteChainRequestHandler explainRewriteChainRequestHandler;

    public QuerqyRewriterRequestHandler() {
        explainRewriteChainRequestHandler =
                new ExplainRewriteChainRequestHandler(this);
    }

    @Override
    public void init(final NamedList args) {
        this.initArgs = args;
    }

    @Override
    public void handleRequest(final SolrQueryRequest req, final SolrQueryResponse rsp) {
        final Map<String, RewriterFactoryContext> rewriters = rewriterContainer.rewriters;
        final Map<String, Object> result = new HashMap<>();
        final Map<String, Map<String, Object>> rewritersResult = rewriters.entrySet().stream().collect(
                toMap(Map.Entry::getKey, entry -> {

                    final Map<String, Object> rewriterMap = new LinkedHashMap<>(2);
                    final String id = entry.getKey();
                    rewriterMap.put("id", id);
                    final String queryType = req.getParams().get(CommonParams.QT);
                    final String prefix = queryType == null ? req.getPath() : queryType;
                    rewriterMap.put("path", prefix.endsWith("/") ? prefix + id : prefix + "/" + id);
                    return rewriterMap;

            }));

            result.put("rewriters", rewritersResult);
            rsp.add("response", result);
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

    public enum SolrMode {
        CLOUD(ZK, IN_MEMORY),
        STANDALONE(CONF_DIR, IN_MEMORY, INDEX);

        private final RewriterStorageType defaultRewriterStorageType;

        private final EnumSet<RewriterStorageType> supportedRewriterStorageTypes;

        SolrMode(final RewriterStorageType defaultRewriterStorageType, final RewriterStorageType... otherSupportedRewriterStorageTypes) {
            this.defaultRewriterStorageType = defaultRewriterStorageType;
            this.supportedRewriterStorageTypes = EnumSet.of(defaultRewriterStorageType, otherSupportedRewriterStorageTypes);
        }

        public RewriterStorageType getDefaultRewriterStorageType() {
            return defaultRewriterStorageType;
        }

        public boolean supportsRewriterStorageType(final RewriterStorageType rewriterStorageType) {
            return supportedRewriterStorageTypes.contains(rewriterStorageType);
        }
    }

    public enum RewriterStorageType {
        IN_MEMORY("inMemory", false),
        CONF_DIR("confDir", true),
        INDEX("index", true),
        ZK("zk", true);

        private final String configValue;
        private final boolean isPersisting;

        RewriterStorageType(final String configValue, final boolean isPersisting) {
            this.configValue = configValue;
            this.isPersisting = isPersisting;
        }

        public boolean isPersisting() {
            return isPersisting;
        }

        public static RewriterStorageType rewriterStorageTypeFromString(final String str) {
            return Arrays.stream(values())
                    .filter(v -> v.configValue.equals(str))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown rewriter storage type: " + str));
        }

        public static RewriterStorageType fromConfig(final NamedList<?> initArgs, final SolrMode solrMode) {
            // Option key "inMemory=true" is deprecated. Will be removed in future versions.
            // New option key is "rewriterStorage=inMemory".
            final Optional<Boolean> inMemory = Optional.ofNullable((Boolean) initArgs.get("inMemory"));
            final Optional<String> rewriterStorageStr = Optional.ofNullable((String) initArgs.get("rewriterStorage"));
            RewriterStorageType rewriterStorageType;
            if (inMemory.isPresent() && Boolean.TRUE.equals(inMemory.get())) {
                rewriterStorageType = IN_MEMORY;
                LOG.warn("You are using a deprecated configuration option: <bool name=\"inMemory\">true</bool>. Please use <str name=\"rewriterStorage\">inMemory</str> instead.");
            } else if (rewriterStorageStr.isPresent()) {
                rewriterStorageType = rewriterStorageTypeFromString(rewriterStorageStr.get());
            } else {
                rewriterStorageType = solrMode.getDefaultRewriterStorageType();
            }

            if (solrMode.supportsRewriterStorageType(rewriterStorageType)) {
                LOG.info("Using rewriter storage: {}", rewriterStorageType);
            } else {
                throw new IllegalArgumentException("Rewriter storage " + rewriterStorageType + " is not supported for solr mode " + solrMode);
            }
            return rewriterStorageType;
        }
    }

    @Override
    public void inform(final SolrCore core) {

        final SolrResourceLoader resourceLoader = core.getResourceLoader();
        Map<String, Sink> sinks = loadSinks(resourceLoader);

        final SolrMode solrMode = resourceLoader instanceof ZkSolrResourceLoader ? CLOUD : STANDALONE;

        rewriterStorageType = RewriterStorageType.fromConfig(initArgs, solrMode);

        switch (rewriterStorageType) {
            case IN_MEMORY:
                rewriterContainer = new InMemoryRewriteContainer(core, resourceLoader, sinks);
                break;
            case CONF_DIR:
                rewriterContainer = new StandAloneRewriterContainer(core, resourceLoader, sinks);
                break;
            case INDEX:
                rewriterContainer = new IndexRewriterContainer(core, resourceLoader, sinks);
                break;
            case ZK:
                assert resourceLoader instanceof ZkSolrResourceLoader;
                rewriterContainer = new ZkRewriterContainer(core, (ZkSolrResourceLoader) resourceLoader, sinks);
                break;
        }

        rewriterContainer.init(initArgs);
    }

    public Map<String, Sink> loadSinks(final SolrResourceLoader resourceLoader) {

        final Map<String, Sink> sinks = new HashMap<>();
        sinks.put("response", new ResponseSink());

        final NamedList<?> loggingConfig = (NamedList<?>) initArgs.get("infoLogging");

        if (loggingConfig != null) {

            @SuppressWarnings("unchecked")
            final List<NamedList<?>> sinkConfigs = (List<NamedList<?>>) loggingConfig.getAll("sink");


            if (sinkConfigs != null) {

                for (NamedList<?> config : sinkConfigs) {

                    final Sink sink = resourceLoader.newInstance((String) config.get("class"), Sink.class);

                    final String id = (String) config.get("id");
                    if (sinks.put(id, sink) != null) {
                        throw new IllegalStateException("Sink id is not unique: " + id);
                    }
                }
            }
        }

        return Collections.unmodifiableMap(sinks);

    }


    /**
     * This check is used for validation as long as we are still allowing rewriters to be configured in solrconfig.xml
     * @return true if rewriter configs sent to this handler will be persisted
     */
    @Deprecated
    public boolean isPersistingRewriters() {
        return rewriterStorageType.isPersisting();
    }

    /**
     * Called by the * Called by the {@link IndexRewriterContainerListener}.
     * <p>
     * <em>Note: This is only relevant when using the {@link IndexRewriterContainerListener}.</em>
     *
     * @param newConfigurationSearcher current searcher for reading the updated rewriter config from the config core
     */
    void notifyRewriterConfigChanged(final SolrIndexSearcher newConfigurationSearcher) {
        if (rewriterContainer instanceof IndexRewriterContainer) {
            try {
                ((IndexRewriterContainer) rewriterContainer).reloadRewriterConfig(newConfigurationSearcher);
            } catch (IOException e) {
                LOG.error("Failed to load all rewriter data", e);
            }
        }
    }

    public Optional<RewriterFactoryContext> getRewriterFactory(final String rewriterId) {
        return rewriterContainer.getRewriterFactory(rewriterId);
    }

    public synchronized Collection<RewriterFactoryContext> getRewriterFactories(final RewriterContainer.RewritersChangeListener listener) {
        return rewriterContainer.getRewriterFactories(listener);
    }


    @Override
    public SolrRequestHandler getSubHandler(final String subPath) {

        if (PATH_EXPLAIN_CHAIN.equals(subPath)) {
            return explainRewriteChainRequestHandler;
        }

        return new SolrRequestHandler() {
            @Override
            public void initializeMetrics(final SolrMetricsContext parentContext, final String scope) {

            }

            @Override
            public SolrMetricsContext getSolrMetricsContext() {
                return null;
            }

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
                            final Map<String, Object> definition = rewriterContainer.readRewriterDefinition(rewriterId);
                            final Map<String, Object> conf = new LinkedHashMap<>(3);
                            conf.put("id", rewriterId);
                            final String queryType = req.getParams().get(CommonParams.QT);
                            conf.put("path", queryType == null ? req.getPath() : queryType);
                            conf.put("definition", definition);
                            rsp.add("rewriter", conf);
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
