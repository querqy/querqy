package querqy.solr;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.SolrCache;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import querqy.lucene.rewrite.cache.CacheKey;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.lucene.rewrite.cache.TermQueryCacheValue;
import querqy.parser.QuerqyParser;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.RewriteChain;
import querqy.rewrite.RewriterFactory;

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
    
    public abstract QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req, TermQueryCache termQueryCache);

    @Override
    public void init(@SuppressWarnings("rawtypes") NamedList args) {
        this.initArgs = args;
    }

    @Override
    public void inform(final ResourceLoader loader) throws IOException {

        rewriteChain = loadRewriteChain(loader);
      
        termQueryCacheName = (String) initArgs.get(CONF_CACHE_NAME);
        
        Boolean updateCache = initArgs.getBooleanArg(CONF_CACHE_UPDATE);
        if (termQueryCacheName == null && updateCache != null) {
            throw new IOException("Configuration property " + CONF_CACHE_NAME + " required if " + CONF_CACHE_UPDATE + " is set");
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
            if (parserConfig == null) {
                throw new IOException("Missing querqy parser configuration");
            }

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
   private RewriteChain loadRewriteChain(ResourceLoader loader) throws IOException {

      NamedList<?> chainConfig = (NamedList<?>) initArgs.get("rewriteChain");
      List<RewriterFactory> factories = new LinkedList<>();

      if (chainConfig != null) {

         @SuppressWarnings("unchecked")
         List<NamedList<?>> rewriterConfigs = (List<NamedList<?>>) chainConfig.getAll("rewriter");
         if (rewriterConfigs != null) {
            for (NamedList<?> config : rewriterConfigs) {
               RewriterFactoryAdapter factory = loader.newInstance((String) config.get("class"),
                     RewriterFactoryAdapter.class);
               factories.add(factory.createRewriterFactory(config, loader));
            }
         }
      }
      
      return new RewriteChain(factories);
      
   }

   protected QuerqyParser createQuerqyParser(String qstr, SolrParams localParams, SolrParams params,
         SolrQueryRequest req) {
      return querqyParserFactory.createParser(qstr, localParams, params, req);
   }

   @Override
   public final QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
       
       if (termQueryCacheName == null) {
           return createParser(qstr, localParams, params, req, null);
       } else {
       
           @SuppressWarnings("unchecked")
           SolrCache<CacheKey, TermQueryCacheValue> solrCache = req.getSearcher().getCache(termQueryCacheName);
           if (solrCache == null) {
               logger.warn("Missing Solr cache {}", termQueryCacheName);
               return createParser(qstr, localParams, params, req, null);
           } else {
               return createParser(qstr, localParams, params, req, new SolrTermQueryCacheAdapter(ignoreTermQueryCacheUpdates, solrCache));
           }
           
       }
   }
   

   
   public RewriteChain getRewriteChain() {
       return rewriteChain;
   }

}
