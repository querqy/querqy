package querqy.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.SolrCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import querqy.lucene.rewrite.cache.CacheKey;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.lucene.rewrite.cache.TermQueryCacheValue;
import querqy.parser.QuerqyParser;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.RewriteChain;
import querqy.rewrite.RewriterFactory;
import querqy.infologging.InfoLogging;
import querqy.infologging.Sink;
import querqy.lucene.GZIPAwareResourceLoader;

/**
 * Abstract superclass for QuerqyDismaxQParserPlugins.
 */
public abstract class AbstractQuerqyDismaxQParserPlugin extends QParserPlugin implements ResourceLoaderAware {

    public static final String CONF_CACHE_NAME = "termQueryCache.name";
    public static final String CONF_CACHE_UPDATE = "termQueryCache.update";

    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected NamedList<?> initArgs = null;
    protected RewriteChain rewriteChain = null;

    protected SolrQuerqyParserFactory querqyParserFactory = null;
    protected String termQueryCacheName = null;
    protected boolean ignoreTermQueryCacheUpdates = true;
    protected InfoLogging infoLogging;

    public abstract QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req,
                                         InfoLogging tracking, TermQueryCache termQueryCache);

    @Override
    public void init(final @SuppressWarnings("rawtypes") NamedList args) {
        this.initArgs = args;
    }

    @Override
    public void inform(final ResourceLoader solrResourceLoader) throws IOException {
        ResourceLoader loader = new GZIPAwareResourceLoader(solrResourceLoader);

        rewriteChain = loadRewriteChain(loader);
        infoLogging = loadInfoLogging(loader);

        termQueryCacheName = (String) initArgs.get(CONF_CACHE_NAME);

        final Boolean updateCache = initArgs.getBooleanArg(CONF_CACHE_UPDATE);
        if (termQueryCacheName == null && updateCache != null) {
            throw new IOException("Configuration property " + CONF_CACHE_NAME + " required if " + CONF_CACHE_UPDATE +
                    " is set");
        }

        ignoreTermQueryCacheUpdates = (updateCache != null) && !updateCache;

        this.querqyParserFactory = loadSolrQuerqyParserFactory(loader, initArgs);
    }

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

    /**
     * Loads the whole {@link RewriteChain}s from the args and returns a list of
     * them.
     */
    private RewriteChain loadRewriteChain(final ResourceLoader loader) throws IOException {

        final NamedList<?> chainConfig = (NamedList<?>) initArgs.get("rewriteChain");
        final List<RewriterFactory> factories = new ArrayList<>();

        if (chainConfig != null) {

            @SuppressWarnings("unchecked")
            final List<NamedList<?>> rewriterConfigs = (List<NamedList<?>>) chainConfig.getAll("rewriter");
            if (rewriterConfigs != null) {

                int count = 0;
                final Set<String> seenRewriterIds = new HashSet<>(rewriterConfigs.size());

                for (NamedList<?> config : rewriterConfigs) {

                    final String className = (String) config.get("class");

                    @SuppressWarnings("unchecked")
                    final FactoryAdapter<RewriterFactory> factoryAdapter = loader
                            .newInstance(className, FactoryAdapter.class);

                    final String idConf = (String) config.get("id");
                    final String id = idConf == null
                            ? factoryAdapter.getCreatedClass().getClass().getName() + "#" + count : idConf;

                    final RewriterFactory factory = factoryAdapter.createFactory(id, config, loader);
                    if (!seenRewriterIds.add(factory.getRewriterId())) {
                        throw new IllegalStateException("Rewriter id is not unique: " + id);
                    }

                    factories.add(factory);
                    count++;
                }
            }
        }

        return new RewriteChain(factories);

    }

    private InfoLogging loadInfoLogging(final ResourceLoader loader) throws IOException {

        final NamedList<?> loggingConfig = (NamedList<?>) initArgs.get("infoLogging");

        if (rewriteChain != null && loggingConfig != null) {

            @SuppressWarnings("unchecked")
            final List<NamedList<?>> sinkConfigs = (List<NamedList<?>>) loggingConfig.getAll("sink");
            final Map<String, Sink> sinks = new HashMap<>();

            if (sinkConfigs != null) {

                for (NamedList<?> config : sinkConfigs) {

                    final Sink sink = loader.newInstance((String) config.get("class"), Sink.class);

                    final String id = (String) config.get("id");
                    if (sinks.put(id, sink) != null) {
                        throw new IllegalStateException("Sink id is not unique: " + id);
                    }

                }
            }

            @SuppressWarnings("unchecked")
            final List<NamedList<?>> mappingConfigs = (List<NamedList<?>>) loggingConfig.getAll("mapping");
            final Map<String, List<Sink>> mappings = new HashMap<>();

            if (mappingConfigs != null) {


                for (NamedList<?> config : mappingConfigs) {

                    final String rewriterId = (String) config.get("rewriter");
                    if (rewriterId == null) {
                        throw new IOException("Missing rewriter in infoLogging mapping");
                    }
                    if (rewriteChain.getFactory(rewriterId) == null) {
                        throw new IOException("infoLogging mapping references non-existent rewriter " + rewriterId);
                    }

                    final String sinkId = (String) config.get("sink");
                    if (sinkId == null) {
                        throw new IOException("Missing sink in infoLogging mapping");
                    }

                    final Sink sink = sinks.get(sinkId);
                    if (sink == null) {
                        throw new IOException("infoLogging mapping references non-existent sink " + sinkId);
                    }

                    mappings.computeIfAbsent(rewriterId, id -> new ArrayList<>()).add(sink);

                }
            }

            return new InfoLogging(mappings);

        } else {
            return null;
        }
    }

    protected QuerqyParser createQuerqyParser(final String qstr, final SolrParams localParams, final SolrParams params,
                                             final SolrQueryRequest req) {
        return querqyParserFactory.createParser(qstr, localParams, params, req);
    }

    @Override
    public final QParser createParser(final String qstr, final SolrParams localParams, final SolrParams params,
                                      SolrQueryRequest req) {

        if (termQueryCacheName == null) {
            return createParser(qstr, localParams, params, req, infoLogging, null);
        } else {

            @SuppressWarnings("unchecked")
            final SolrCache<CacheKey, TermQueryCacheValue> solrCache = req.getSearcher().getCache(termQueryCacheName);
            if (solrCache == null) {
                logger.warn("Missing Solr cache {}", termQueryCacheName);
                return createParser(qstr, localParams, params, req, infoLogging, null);
            } else {
                return createParser(qstr, localParams, params, req, infoLogging,
                        new SolrTermQueryCacheAdapter(ignoreTermQueryCacheUpdates, solrCache));
            }

        }
    }



    public RewriteChain getRewriteChain() {
        return rewriteChain;
    }

}
