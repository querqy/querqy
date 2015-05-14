/**
 * 
 */
package querqy.solr;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.solr.core.AbstractSolrEventListener;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrCache;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import querqy.lucene.rewrite.LuceneQueryBuilder;
import querqy.lucene.rewrite.NeverMatchQueryFactory;
import querqy.lucene.rewrite.TermSubQueryFactory;
import querqy.lucene.rewrite.cache.CacheKey;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.lucene.rewrite.cache.TermQueryCacheValue;
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
    
    protected Set<String> getPreloadFields() {
        // REVISIT: we don't need the boost factors as could will be overridden per request
       String fieldConf = (String) getArgs().get(CONF_PRELOAD_FIELDS); 
       return QuerqyDismaxQParser.parseFieldBoosts(fieldConf, 1f).keySet();
    }
    
    protected AbstractQuerqyDismaxQParserPlugin getQParserPlugin() {
        String parserName = (String) getArgs().get(CONF_Q_PARSER_PLUGIN); 
        if (parserName == null) {
            throw new RuntimeException("Missing configuration property: " + CONF_Q_PARSER_PLUGIN);
        }
        AbstractQuerqyDismaxQParserPlugin qParserPlugin = (AbstractQuerqyDismaxQParserPlugin) getCore().getQueryPlugin(parserName);
        if (qParserPlugin == null) {
            throw new RuntimeException("No query parser plugin for name '" + parserName + "'");
        }
        return qParserPlugin;
    }
    
    protected TermQueryCache getCache(SolrIndexSearcher searcher) {
        String cacheName = (String) getArgs().get(CONF_CACHE_NAME); 
        if (cacheName == null) {
            throw new RuntimeException("Missing configuration property: " + CONF_CACHE_NAME);
        }
        
        @SuppressWarnings("unchecked")
        SolrCache<CacheKey, TermQueryCacheValue> solrCache = searcher.getCache(cacheName);
        if (solrCache == null) {
            throw new RuntimeException("No TermQueryCache for name '" + cacheName + "'");
        }
        
        return new SolrTermQueryCacheAdapter(false, solrCache);
    }
    
    protected boolean isTestForHits() {
        Boolean doTest = getArgs().getBooleanArg(CONF_TEST_FOR_HITS);
        return doTest != null && doTest;
    }
    
    @Override
    public void newSearcher(SolrIndexSearcher newSearcher, SolrIndexSearcher currentSearcher) {
        
        
        TermQueryCache cache = getCache(newSearcher);
        
        Set<String> preloadFields = getPreloadFields();
        
        boolean testForHits = isTestForHits();
        
        LOG.info("Starting preload for Querqy TermQueryCache. Testing for hits: {}", testForHits);
        long t1 = System.currentTimeMillis();
        
        
        AbstractQuerqyDismaxQParserPlugin queryPluginPlugin = getQParserPlugin();
        RewriteChain rewriteChain = queryPluginPlugin.getRewriteChain();
        
        if (rewriteChain != null && !preloadFields.isEmpty()) {
        
            List<RewriterFactory> factories = rewriteChain.getRewriterFactories();
            if (!factories.isEmpty()) {
            
                // REVISIT: split off request-independent code from LuceneQueryBuilder  
                LuceneQueryBuilder luceneQueryBuilder = new LuceneQueryBuilder(null, 
                            newSearcher.getSchema().getQueryAnalyzer(), null, null, 1f, 1f, cache);
            
                for (RewriterFactory factory : factories) {
                    for (Term term: factory.getGenerableTerms()) {
                        String field = term.getField();
                        if (field != null) {
                            if (preloadFields.contains(field)) {
                                preloadTerm(newSearcher, luceneQueryBuilder, field, term, testForHits, cache);
                            }
                        } else {
                            for (String fieldname : preloadFields) {
                                preloadTerm(newSearcher, luceneQueryBuilder, fieldname, term, testForHits, cache);
                            }
                        }
                    }
                }
                
            }
            
        }
        
        if (LOG.isInfoEnabled()) {
            long t2 = System.currentTimeMillis();
            LOG.info("Finished preload for Querqy TermQueryCache after {}ms", (t2 - t1));
        }
        
    }
    
    
    protected void preloadTerm(IndexSearcher searcher, LuceneQueryBuilder luceneQueryBuilder, String field, Term term, boolean testForHits, TermQueryCache cache) {
        
        try {
        
            TermSubQueryFactory termSubQueryFactory = luceneQueryBuilder.termToFactory(field, term, 1f);
            
            // no need to re-test for hits if we've seen this term before
            if (testForHits && (termSubQueryFactory != null) && (!termSubQueryFactory.isNeverMatchQuery())) {
                Query query = termSubQueryFactory.createQuery(1f, 0.01f, null, false);
                TopDocs topDocs = searcher.search(query, 1);
                if (topDocs.totalHits < 1) {
                    cache.put(new CacheKey(field, term), new TermQueryCacheValue(NeverMatchQueryFactory.FACTORY));
                }
            }
        
        } catch (IOException e) {
            LOG.error("Error preloading term " + term.toString(), e);
        }
    }

}
