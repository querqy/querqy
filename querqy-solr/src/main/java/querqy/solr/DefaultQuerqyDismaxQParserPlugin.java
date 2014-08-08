package querqy.solr;

import java.io.IOException;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;

import querqy.parser.QuerqyParser;

public class DefaultQuerqyDismaxQParserPlugin extends AbstractQuerqyDismaxQParserPlugin {

   protected Class<? extends QuerqyParser> querqyParserClass;

   @Override
   public void inform(ResourceLoader loader) throws IOException {
      super.inform(loader);
      querqyParserClass = loadQuerqyParserClass(loader);
   }

   /**
    * Loads the {@link QuerqyParser} from the args class to caches it.
    */
   private Class<? extends QuerqyParser> loadQuerqyParserClass(ResourceLoader loader) throws IOException {

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
   public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
      try {
         return new QuerqyDismaxQParser(qstr, localParams, params, req, rewriteChain,
               new SolrIndexStats(req.getSearcher()), querqyParserClass.newInstance());
      } catch (InstantiationException | IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }

}
