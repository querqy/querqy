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

import querqy.parser.QuerqyParser;
import querqy.rewrite.RewriteChain;
import querqy.rewrite.RewriterFactory;

/**
 * Abstract superclass for QuerqyDismaxQParserPlugins.
 */
public abstract class AbstractQuerqyDismaxQParserPlugin extends QParserPlugin implements ResourceLoaderAware {

   protected NamedList<?> initArgs = null;
   protected RewriteChain rewriteChain = null;
   protected SolrQuerqyParserFactory querqyParserFactory = null;

   @Override
   public void init(@SuppressWarnings("rawtypes") NamedList args) {
      this.initArgs = args;
   }

   @Override
   public void inform(ResourceLoader loader) throws IOException {

      NamedList<?> parserConfig = (NamedList<?>) initArgs.get("parser");
      if (parserConfig == null) {
         throw new IOException("Missing querqy parser configuration");
      }

      String className = (String) parserConfig.get("factory");
      if (className == null) {
         throw new IOException("Missing attribute 'factory' in querqy parser configuration");
      }

      SolrQuerqyParserFactory factory = loader.newInstance(className, SolrQuerqyParserFactory.class);
      factory.init(parserConfig, loader);

      rewriteChain = loadRewriteChain(loader);

      this.querqyParserFactory = factory;
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
   public abstract QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req);

}
