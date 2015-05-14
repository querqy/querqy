/**
 * 
 */
package querqy.solr;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.solr.core.AbstractSolrEventListener;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrCache;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import querqy.lucene.rewrite.DocumentFrequencyCorrection;
import querqy.lucene.rewrite.LuceneQueryBuilder;
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
    
    @Override
    public void newSearcher(SolrIndexSearcher newSearcher, SolrIndexSearcher currentSearcher) {
        
        TermQueryCache cache = getCache(newSearcher);
        
        Set<String> preloadFields = getPreloadFields();
        
        AbstractQuerqyDismaxQParserPlugin queryPluginPlugin = getQParserPlugin();
        RewriteChain rewriteChain = queryPluginPlugin.getRewriteChain();
        
        if (rewriteChain != null && !preloadFields.isEmpty()) {
        
            List<RewriterFactory> factories = rewriteChain.getRewriterFactories();
            if (!factories.isEmpty()) {
            
                DocumentFrequencyCorrection dfc = null;
                // TODO: split off request-independent code from LuceneQueryBuilder  
                LuceneQueryBuilder luceneQueryBuilder = new LuceneQueryBuilder(dfc, 
                            newSearcher.getSchema().getQueryAnalyzer(), null, null, 1f, 1f, cache);
            
                for (RewriterFactory factory : factories) {
                    for (Term term: factory.getGenerableTerms()) {
                        String field = term.getField();
                        if (field != null) {
                            if (preloadFields.contains(field)) {
                                try {
                                    luceneQueryBuilder.termToFactory(field, term, 1f);
                                } catch (IOException e) {
                                    LOG.error("Error preloading term " + term.toString(), e);
                                }
                            }
                        } else {
                            for (String fieldname : preloadFields) {
                                try {
                                    luceneQueryBuilder.termToFactory(fieldname, term, 1f);
                                } catch (IOException e) {
                                    LOG.error("Error preloading term " + term.toString(), e);
                                }
                            }
                        }
                    }
                }
                
            }
            
        }
        
    }

}
