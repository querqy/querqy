package querqy.solr;

import org.apache.lucene.util.ResourceLoader;
import org.apache.lucene.util.ResourceLoaderAware;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.SolrCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import querqy.lucene.GZIPAwareResourceLoader;
import querqy.lucene.rewrite.cache.CacheKey;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.lucene.rewrite.cache.TermQueryCacheValue;
import querqy.lucene.rewrite.infologging.InfoLogging;
import querqy.lucene.rewrite.infologging.MultiSinkInfoLogging;
import querqy.lucene.rewrite.infologging.Sink;
import querqy.parser.QuerqyParser;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.RewriteChain;
import querqy.rewrite.RewriterFactory;

import java.io.IOException;
import java.util.*;

import static querqy.solr.RewriteLoggingParameters.PARAM_REWRITE_LOGGING_REWRITERS;

public abstract class QuerqyQParserPlugin extends QParserPlugin implements ResourceLoaderAware {

    /**
     * Name of request parameter that contains one or more query IDs. Rewriter IDs must be separated by commas.
     * Rewriters will be applied to the query in the order of their occurrence in the parameter.
     */
    public static final String PARAM_REWRITERS = "querqy.rewriters";

    public static final String CONF_CACHE_NAME = "termQueryCache.name";
    public static final String CONF_CACHE_UPDATE = "termQueryCache.update";
    public static final String CONF_REWRITER_REQUEST_HANDLER = "rewriterRequestHandler";
    public static final String CONF_SKIP_UNKNOWN_REWRITERS = "skipUnknownRewriters";
    public static final String CONF_SKIP_UNKNOWN_REWRITERS_WITH_TYPO = "skipUnkownRewriters";


    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected NamedList<?> initArgs = null;

    protected SolrQuerqyParserFactory querqyParserFactory = null;
    protected String termQueryCacheName = null;
    protected boolean ignoreTermQueryCacheUpdates = true;

    protected String rewriterRequestHandlerName = QuerqyRewriterRequestHandler.DEFAULT_HANDLER_NAME;
    protected boolean skipUnknownRewriter = false;

    @Override
    public void init(final @SuppressWarnings("rawtypes") NamedList args) {
        this.initArgs = args;

        String name = (String) args.get(CONF_REWRITER_REQUEST_HANDLER);

        if (name != null) {
            name = name.trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("'" + CONF_REWRITER_REQUEST_HANDLER + "' must not be empty");
            }
        }

        // This element is not allowed any more here and must be removed
        if (args.get("rewriters") != null) {
            throw new IllegalArgumentException("'rewriters' configuration is no longer allowed in the query parser " +
                    "configuration. You can move the config to a " + ClassicRewriteChainLoader.class + ". This option" +
                    " will be removed in the near future and you will have to use the rewriter API. See upgrade info.");
        }

        rewriterRequestHandlerName = name != null ? name : QuerqyRewriterRequestHandler.DEFAULT_HANDLER_NAME;
        Boolean skip = args.getBooleanArg(CONF_SKIP_UNKNOWN_REWRITERS);
        if (skip == null) {
          skip = args.getBooleanArg(CONF_SKIP_UNKNOWN_REWRITERS_WITH_TYPO);
          if (skip != null){
            logger.warn("Querqy 5.0 shipped with a typo in the parameter 'skipUnkownRewriters', please update your parameter to 'skipUnknownRewriters'.");
          }
        }
        skipUnknownRewriter = skip != null ? skip : false;

        logger.info("Initialized Querqy query parser: QuerqyRewriterRequestHandler={},skipUnknownRewriter={}",
                rewriterRequestHandlerName, skipUnknownRewriter);

    }

    @Override
    public void inform(final ResourceLoader solrResourceLoader) throws IOException {

        final ResourceLoader loader = new GZIPAwareResourceLoader(solrResourceLoader);

        termQueryCacheName = (String) initArgs.get(CONF_CACHE_NAME);

        final Boolean updateCache = initArgs.getBooleanArg(CONF_CACHE_UPDATE);
        if (termQueryCacheName == null && updateCache != null) {
            throw new IOException("Configuration property " + CONF_CACHE_NAME + " required if " + CONF_CACHE_UPDATE +
                    " is set");
        }

        ignoreTermQueryCacheUpdates = (updateCache != null) && !updateCache;

        this.querqyParserFactory = loadSolrQuerqyParserFactory(loader, initArgs);
    }

    public abstract QParser createParser(final String qstr, final SolrParams localParams, final SolrParams params,
                                         final SolrQueryRequest req, final RewriteChain rewriteChain,
                                         final InfoLogging infoLogging, final TermQueryCache termQueryCache);


    protected SolrQuerqyParserFactory loadSolrQuerqyParserFactory(final ResourceLoader loader,
                                                                  final NamedList<?> args) throws IOException {

        final Object parserConfig = args.get("parser");
        if (parserConfig == null) {

            final SimpleQuerqyQParserFactory factory = new SimpleQuerqyQParserFactory();
            factory.setQuerqyParserClass(WhiteSpaceQuerqyParser.class);
            return factory;

        } else if (parserConfig instanceof String) {

            final SimpleQuerqyQParserFactory factory = new SimpleQuerqyQParserFactory();

            factory.init((String) parserConfig, loader);

            return factory;

        } else {

            NamedList<?> parserConfigMap = (NamedList<?>) parserConfig;

            final String className = (String) parserConfigMap.get("factory");

            final SolrQuerqyParserFactory factory = className == null
                    ? new SimpleQuerqyQParserFactory()
                    : loader.newInstance(className, SolrQuerqyParserFactory.class);
            factory.init(parserConfigMap, loader);

            return factory;

        }
    }


    protected QuerqyParser createQuerqyParser(final String qstr, final SolrParams localParams, final SolrParams params,
                                              final SolrQueryRequest req) {
        return querqyParserFactory.createParser(qstr, localParams, params, req);
    }

    @Override
    public final QParser createParser(final String qstr, final SolrParams localParams, final SolrParams params,
                                      final SolrQueryRequest req) {
        String rewritersParam = null;
        if (localParams != null) {
            rewritersParam = localParams.get(PARAM_REWRITERS);

        }
        if (rewritersParam == null) {
            rewritersParam = params.get(PARAM_REWRITERS);
        }

        final String loggingRewritersParam = params.get(PARAM_REWRITE_LOGGING_REWRITERS);
        final boolean hasFullRewriteLogging = "*".equals(loggingRewritersParam);
        final Set<String> loggingRewriters = new HashSet<>();
        if (!hasFullRewriteLogging && loggingRewritersParam != null) {
            Collections.addAll(loggingRewriters, loggingRewritersParam.split(","));
        }

        final RewriteChain rewriteChain;
        final InfoLogging infoLogging;
        if (rewritersParam != null) {

            final QuerqyRewriterRequestHandler rewriterRequestHandler = getQuerqyRequestHandler(req.getCore());
            final String[] rewriterIds = rewritersParam.split(",");
            final List<RewriterFactory> factories = new ArrayList<>(rewriterIds.length);
            final Map<String, List<Sink>> sinkMappings = new HashMap<>(rewriterIds.length);

            for (final String rewriterId: rewriterIds) {

                final Optional<RewriterFactoryContext> factoryOpt = rewriterRequestHandler
                        .getRewriterFactory(rewriterId.trim());

                if (factoryOpt.isPresent()) {
                    final RewriterFactoryContext context = factoryOpt.get();
                    factories.add(context.getRewriterFactory());

                    if (hasFullRewriteLogging || loggingRewriters.contains(rewriterId)) {
                        final List<Sink> sinks = context.getSinks();
                        if (sinks != null && (!sinks.isEmpty())) {
                            sinkMappings.put(rewriterId, sinks);
                        }
                    }
                } else if (skipUnknownRewriter){
                    logger.warn("Skipping unknown rewriter: {}", rewriterId);
                } else {
                    throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "No such rewriter: " + rewriterId);
                }

            }
            rewriteChain = new RewriteChain(factories);
            infoLogging = new MultiSinkInfoLogging(sinkMappings);

        } else {
            rewriteChain = new RewriteChain();
            infoLogging = new MultiSinkInfoLogging(Collections.emptyMap()); // TODO: just use null?
        }


        if (termQueryCacheName == null) {
            return createParser(qstr, localParams, params, req, rewriteChain, infoLogging, null);
        } else {

            @SuppressWarnings("unchecked")
            final SolrCache<CacheKey, TermQueryCacheValue> solrCache = req.getSearcher().getCache(termQueryCacheName);
            if (solrCache == null) {
                logger.warn("Missing Solr cache {}", termQueryCacheName);
                return createParser(qstr, localParams, params, req, rewriteChain, infoLogging, null);
            } else {
                return createParser(qstr, localParams, params, req, rewriteChain, infoLogging,
                        new SolrTermQueryCacheAdapter(ignoreTermQueryCacheUpdates, solrCache));
            }

        }
    }

    private QuerqyRewriterRequestHandler getQuerqyRequestHandler(final SolrCore core){
        final SolrRequestHandler requestHandler = core.getRequestHandler(rewriterRequestHandlerName);
        if (requestHandler == null) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
                    "Could not find QuerqyRewriterRequestHandler for name " + rewriterRequestHandlerName);
        }
        if (!(requestHandler instanceof QuerqyRewriterRequestHandler)) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
                    rewriterRequestHandlerName + " is not a " + QuerqyRewriterRequestHandler.class.getName());
        }

        return (QuerqyRewriterRequestHandler) requestHandler;
    }
}
