package querqy.solr;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.AbstractSolrEventListener;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.search.SolrCache;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import querqy.lucene.rewrite.ConstantFieldBoost;
import querqy.lucene.rewrite.LuceneTermQueryBuilder;
import querqy.lucene.rewrite.NeverMatchQueryFactory;
import querqy.lucene.rewrite.TermSubQueryBuilder;
import querqy.lucene.rewrite.TermSubQueryFactory;
import querqy.lucene.rewrite.cache.CacheKey;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.lucene.rewrite.cache.TermQueryCacheValue;
import querqy.lucene.rewrite.prms.PRMSQuery;
import querqy.model.Term;
import querqy.rewrite.RewriterFactory;

/**
 * @author rene
 *
 */
public class TermQueryCachePreloader extends AbstractSolrEventListener implements
        RewriterContainer.RewritersChangeListener {
    
    static final Logger LOG = LoggerFactory.getLogger(TermQueryCachePreloader.class); 
    
    public static final String CONF_PRELOAD_FIELDS = "fields";
    public static final String CONF_CACHE_NAME = "cacheName";
    public static final String CONF_TEST_FOR_HITS = "testForHits";
    public static final String CONF_REWRITER_REQUEST_HANDLER = "rewriterRequestHandler";

    private Set<String> preloadFields = Collections.emptySet();
    private String cacheName = null;
    private boolean testForHits = false;
    private String rewriterRequestHandlerName = QuerqyRewriterRequestHandler.DEFAULT_HANDLER_NAME;
    
    public TermQueryCachePreloader(final SolrCore core) {
        super(core);
    }

    @Override
    public void init(final NamedList args) {
        super.init(args);
        configurePreloadFields(args);
        configureCacheName(args);
        configureTestForHits(args);
        configureRewriterRequestHandlerName(args);
    }

    private void configureRewriterRequestHandlerName(final NamedList args) {
        
        String name = (String) args.get(CONF_REWRITER_REQUEST_HANDLER);

        if (name != null) {
            name = name.trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("'" + CONF_REWRITER_REQUEST_HANDLER + "' must not be empty");
            }
        }

        rewriterRequestHandlerName = name != null ? name : QuerqyRewriterRequestHandler.DEFAULT_HANDLER_NAME;

    }

    @Override
    public void newSearcher(final SolrIndexSearcher newSearcher, final SolrIndexSearcher currentSearcher) {
        final SolrRequestHandler rewriterRequestHandler = getCore().getRequestHandler(rewriterRequestHandlerName);
        if (rewriterRequestHandler == null) {
            throw new RuntimeException("No '" + QuerqyRewriterRequestHandler.class.getName()
                    + "' configured for name + '" + CONF_REWRITER_REQUEST_HANDLER + "'");
        }

        preload(newSearcher, ((QuerqyRewriterRequestHandler) rewriterRequestHandler).getRewriterFactories(this));
    }

    protected void preload(final SolrIndexSearcher searcher, final Collection<RewriterFactoryContext> rewriterFactories ) {

        if (rewriterFactories.isEmpty()) {
            LOG.info("TermQueryCachePreloader loaded. No rewriters yet");
            return;
        }

        final TermQueryCache cache = getCache(searcher);

        LOG.info("Starting preload for Querqy TermQueryCache. Testing for hits: {}", testForHits);

        final long t1 = System.currentTimeMillis();

        final TermSubQueryBuilder termSubQueryBuilder = new TermSubQueryBuilder(searcher.getSchema().getQueryAnalyzer(),
                cache);
        for (final RewriterFactoryContext factoryContext : rewriterFactories) {
            for (final Term term: factoryContext.getRewriterFactory().getCacheableGenerableTerms()) {
                final String field = term.getField();
                if (field != null) {
                    if (preloadFields.contains(field)) {
                        preloadTerm(searcher, termSubQueryBuilder, field, term, testForHits, cache);
                    }
                } else {
                    for (final String fieldname : preloadFields) {
                        preloadTerm(searcher, termSubQueryBuilder, fieldname, term, testForHits, cache);
                    }
                }
            }
        }

        if (LOG.isInfoEnabled()) {
            final long t2 = System.currentTimeMillis();
            LOG.info("Finished preload for Querqy TermQueryCache after {}ms", (t2 - t1));
        }

    }

    protected void preloadTerm(final IndexSearcher searcher, final TermSubQueryBuilder termSubQueryBuilder,
                               final String field, final Term term, final boolean testForHits,
                               final TermQueryCache cache) {
        
        try {
            
            // luceneQueryBuilder.termToFactory creates the query and caches it (without the boost)
            final TermSubQueryFactory termSubQueryFactory
                    = termSubQueryBuilder.termToFactory(field, term, ConstantFieldBoost.NORM_BOOST);
            
            // test the query for hits and override the cache value with a factory that creates a query that never matches
            // --> this query will never be executed against the index again
            
            // no need to re-test for hits if we've seen this term before
            if (testForHits && (termSubQueryFactory != null) && (!termSubQueryFactory.isNeverMatchQuery())) {
                final Query query = termSubQueryFactory
                        .createQuery(ConstantFieldBoost.NORM_BOOST, new LuceneTermQueryBuilder());
                final TopDocs topDocs = searcher.search(query, 1);
                if (topDocs.totalHits.value < 1) {
                    cache.put(new CacheKey(field, term),
                            new TermQueryCacheValue(NeverMatchQueryFactory.FACTORY, PRMSQuery.NEVER_MATCH_PRMS_QUERY));
                }
            }
        
        } catch (final IOException e) {
            LOG.error("Error preloading term " + term.toString(), e);
        }
    }


    @Override
    public void rewritersChanged(final SolrIndexSearcher indexSearcher, final Set<RewriterFactoryContext> allRewriters) {
        preload(indexSearcher, allRewriters);
    }

    private void configurePreloadFields(final NamedList args) {
        final String fieldConf = (String) args.get(CONF_PRELOAD_FIELDS);
        if (fieldConf == null || fieldConf.trim().isEmpty()) {
            throw new IllegalArgumentException("'" + CONF_PRELOAD_FIELDS + "' must be set in configuration");
        }
        // REVISIT: we don't need the boost factors as they could be overridden per request
        final Map<String, Float> fieldBoosts = DismaxSearchEngineRequestAdapter
                .parseFieldBoosts(new String[]{fieldConf}, 1f);
        if (fieldBoosts.isEmpty()) {
            throw new IllegalArgumentException("'" + CONF_PRELOAD_FIELDS + "' must contain one or more field names in " +
                    "configuration");
        }
        preloadFields = fieldBoosts.keySet();
    }

    private void configureCacheName(final NamedList args) {
        final String name = (String) args.get(CONF_CACHE_NAME);
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("'" + CONF_CACHE_NAME + "' must be set in configuration");
        }
        cacheName = name.trim();
    }

    private void configureTestForHits(final NamedList args) {

        final Boolean doTest = args.getBooleanArg(CONF_TEST_FOR_HITS);
        testForHits = doTest != null && doTest;

    }

    private TermQueryCache getCache(final SolrIndexSearcher searcher) {

        @SuppressWarnings("unchecked")
        final SolrCache<CacheKey, TermQueryCacheValue> solrCache = searcher.getCache(cacheName);
        if (solrCache == null) {
            throw new RuntimeException("No TermQueryCache for name '" + cacheName + "'");
        }

        return new SolrTermQueryCacheAdapter(false, solrCache);
    }

}
