package querqy.solr;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.solr.core.AbstractSolrEventListener;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrCache;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import querqy.lucene.rewrite.LuceneTermQueryBuilder;
import querqy.lucene.rewrite.*;
import querqy.lucene.rewrite.cache.CacheKey;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.lucene.rewrite.cache.TermQueryCacheValue;
import querqy.lucene.rewrite.prms.PRMSQuery;
import querqy.model.Term;
import querqy.rewrite.RewriteChain;
import querqy.rewrite.RewriterFactory;

/**
 * @author rene
 *
 */
public class TermQueryCachePreloader extends AbstractSolrEventListener {
    
    static final Logger LOG = LoggerFactory.getLogger(TermQueryCachePreloader.class); 
    
    public static final String CONF_Q_PARSER_PLUGIN = "qParserPlugin";
    
    public static final String CONF_PRELOAD_FIELDS = "fields";
    
    public static final String CONF_CACHE_NAME = "cacheName";
    
    public static final String CONF_TEST_FOR_HITS = "testForHits";
    
    public TermQueryCachePreloader(SolrCore core) {
        super(core);
    }
    
    protected Map<String, Float> getPreloadFields() {
        // REVISIT: we don't need the boost factors as they could be overridden per request
       final String fieldConf = (String) getArgs().get(CONF_PRELOAD_FIELDS);
       return DismaxSearchEngineRequestAdapter.parseFieldBoosts(new String[] { fieldConf }, 1f);
    }
    
    protected AbstractQuerqyDismaxQParserPlugin getQParserPlugin() {
        final String parserName = (String) getArgs().get(CONF_Q_PARSER_PLUGIN);
        if (parserName == null) {
            throw new RuntimeException("Missing configuration property: " + CONF_Q_PARSER_PLUGIN);
        }
        final AbstractQuerqyDismaxQParserPlugin qParserPlugin = (AbstractQuerqyDismaxQParserPlugin) getCore()
                .getQueryPlugin(parserName);
        if (qParserPlugin == null) {
            throw new RuntimeException("No query parser plugin for name '" + parserName + "'");
        }
        return qParserPlugin;
    }
    
    protected TermQueryCache getCache(final SolrIndexSearcher searcher) {
        final String cacheName = (String) getArgs().get(CONF_CACHE_NAME);
        if (cacheName == null) {
            throw new RuntimeException("Missing configuration property: " + CONF_CACHE_NAME);
        }
        
        @SuppressWarnings("unchecked")
        final SolrCache<CacheKey, TermQueryCacheValue> solrCache = searcher.getCache(cacheName);
        if (solrCache == null) {
            throw new RuntimeException("No TermQueryCache for name '" + cacheName + "'");
        }
        
        return new SolrTermQueryCacheAdapter(false, solrCache);
    }
    
    protected boolean isTestForHits() {
        final Boolean doTest = getArgs().getBooleanArg(CONF_TEST_FOR_HITS);
        return doTest != null && doTest;
    }
    
    @Override
    public void newSearcher(final SolrIndexSearcher newSearcher, final SolrIndexSearcher currentSearcher) {

        final TermQueryCache cache = getCache(newSearcher);
        
        final Map<String, Float> preloadFields = getPreloadFields();
        
        final boolean testForHits = isTestForHits();
        
        LOG.info("Starting preload for Querqy TermQueryCache. Testing for hits: {}", testForHits);
        final long t1 = System.currentTimeMillis();
        
        
        final AbstractQuerqyDismaxQParserPlugin queryPluginPlugin = getQParserPlugin();
        final RewriteChain rewriteChain = queryPluginPlugin.getRewriteChain();
        
        if (rewriteChain != null && !preloadFields.isEmpty()) {
        
            final List<RewriterFactory> factories = rewriteChain.getRewriterFactories();
            if (!factories.isEmpty()) {
            
                final TermSubQueryBuilder termSubQueryBuilder = new TermSubQueryBuilder(newSearcher.getSchema().getQueryAnalyzer(), cache);
                for (final RewriterFactory factory : factories) {
                    for (final Term term: factory.getGenerableTerms()) {
                        final String field = term.getField();
                        if (field != null) {
                            if (preloadFields.containsKey(field)) {
                                preloadTerm(newSearcher, termSubQueryBuilder, field, term, testForHits, cache);
                            }
                        } else {
                            for (final String fieldname : preloadFields.keySet()) {
                                preloadTerm(newSearcher, termSubQueryBuilder, fieldname, term, testForHits, cache);
                            }
                        }
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
                        .createQuery(ConstantFieldBoost.NORM_BOOST, 0.01f, new LuceneTermQueryBuilder());
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
    


}
