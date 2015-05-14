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
    
    public static final String CONF_PRELOAD_FIELDS = "fields";
    
    // FIXME:  configure name
    final String parserName = "querqy";
    final String cacheName = "querqyTermQueryCache";

    public TermQueryCachePreloader(SolrCore core) {
        super(core);
    }
    
    protected Set<String> getPreloadFields() {
        // REVISIT: we don't need the boost factors as could will be overridden per request
       String fieldConf = (String) getArgs().get(CONF_PRELOAD_FIELDS); 
       return QuerqyDismaxQParser.parseFieldBoosts(fieldConf, 1f).keySet();
    }
    
    @Override
    public void newSearcher(SolrIndexSearcher newSearcher, SolrIndexSearcher currentSearcher) {
        
        AbstractQuerqyDismaxQParserPlugin queryPlugin = (AbstractQuerqyDismaxQParserPlugin) getCore().getQueryPlugin(parserName);
        if (queryPlugin == null) {
            throw new RuntimeException("No query plugin for name '" + parserName + "'");
        }
        
        @SuppressWarnings("unchecked")
        SolrCache<CacheKey, TermQueryCacheValue> solrCache = newSearcher.getCache(cacheName);
        if (solrCache == null) {
            throw new RuntimeException("No TermQueryCache for name '" + cacheName + "'");
        }
        
        TermQueryCache cache = new TermQueryCacheAdapter(solrCache);
        
        Set<String> preloadFields = getPreloadFields();
        
        RewriteChain rewriteChain = queryPlugin.getRewriteChain();
        
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
                                    // FIXME: log
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            for (String fieldname : preloadFields) {
                                try {
                                    luceneQueryBuilder.termToFactory(fieldname, term, 1f);
                                } catch (IOException e) {
                                    // FIXME: log
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                
            }
            
        }
        
    }

}
