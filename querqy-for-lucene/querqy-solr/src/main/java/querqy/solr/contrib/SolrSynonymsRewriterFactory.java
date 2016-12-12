/**
 * 
 */
package querqy.solr.contrib;

import java.io.IOException;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;

import querqy.lucene.contrib.rewrite.LuceneSynonymsRewriterFactory;
import querqy.rewrite.RewriterFactory;
import querqy.solr.RewriterFactoryAdapter;

/**
 * @author rene
 *
 */
public class SolrSynonymsRewriterFactory implements RewriterFactoryAdapter {

   /*
    * (non-Javadoc)
    * 
    * @see
    * querqy.solr.SolrRewriterFactory#createRewriterFactory(org.apache.solr.
    * common.util.NamedList, org.apache.lucene.analysis.util.ResourceLoader)
    */
   @Override
   public RewriterFactory createRewriterFactory(NamedList<?> args, ResourceLoader resourceLoader) throws IOException {

      Boolean expand = args.getBooleanArg("expand");
      if (expand == null) {
         expand = false;
      }

      Boolean ignoreCase = args.getBooleanArg("ignoreCase");
      if (ignoreCase == null) {
         ignoreCase = true;
      }

      String synonymResoureName = (String) args.get("synonyms");
      if (synonymResoureName == null) {
         throw new IllegalArgumentException("Property 'synonyms' not configured");
      }

      LuceneSynonymsRewriterFactory factory = new LuceneSynonymsRewriterFactory(expand, ignoreCase);
      for (String resource : synonymResoureName.split(",")) {
         resource = resource.trim();
         if (resource.length() > 0) {
            factory.addResource(resourceLoader.openResource(resource));
         }
      }

      factory.build();

      return factory;

   }

}
