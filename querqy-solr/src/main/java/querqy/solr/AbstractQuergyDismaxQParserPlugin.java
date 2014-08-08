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

public abstract class AbstractQuergyDismaxQParserPlugin extends QParserPlugin implements ResourceLoaderAware {

   protected NamedList<?> initArgs = null;
   protected RewriteChain rewriteChain = null;
   protected Class<? extends QuerqyParser> querqyParserClass;

   @Override
   public void init(NamedList args) {
      this.initArgs = args;
   }

   @Override
   public void inform(ResourceLoader loader) throws IOException {
      rewriteChain = loadRewriteChain(loader);
      querqyParserClass = loadQuerqyParserClass(loader);

   }

   public RewriteChain loadRewriteChain(ResourceLoader loader) throws IOException {

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

   public Class<? extends QuerqyParser> loadQuerqyParserClass(ResourceLoader loader) throws IOException {

      NamedList<?> parserConfig = (NamedList<?>) initArgs.get("parser");
      if (parserConfig == null) {
         throw new IOException("Missing querqy parser configuration");
      }

      String className = (String) parserConfig.get("class");
      if (className == null) {
         throw new IOException("Missing attribute 'class' in querqy parser configuration");
      }

      return loader.findClass(className, QuerqyParser.class);
   }

   @Override
   public abstract QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req);

}
