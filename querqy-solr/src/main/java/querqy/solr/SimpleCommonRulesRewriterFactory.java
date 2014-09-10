/**
 * 
 */
package querqy.solr;

import java.io.IOException;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;

import querqy.parser.QuerqyParserFactory;
import querqy.rewrite.RewriterFactory;

/**
 * @author rene
 *
 */
public class SimpleCommonRulesRewriterFactory implements RewriterFactoryAdapter {

   /*
    * (non-Javadoc)
    * 
    * @see
    * querqy.solr.RewriterFactoryAdapter#createRewriterFactory(org.apache.solr
    * .common.util.NamedList, org.apache.lucene.analysis.util.ResourceLoader)
    */
   @Override
   public RewriterFactory createRewriterFactory(NamedList<?> args,
         ResourceLoader resourceLoader) throws IOException {
      String rulesResourceName = (String) args.get("rules");
      if (rulesResourceName == null) {
         throw new IllegalArgumentException("Property 'rules' not configured");
      }
      
      Boolean ignoreCase = args.getBooleanArg("ignoreCase");

      // querqy parser for queries that are part of the instructions in the
      // rules
      String rulesQuerqyParser = (String) args.get("querqyParser");
      QuerqyParserFactory querqyParser = null;
      if (rulesQuerqyParser != null) {
         rulesQuerqyParser = rulesQuerqyParser.trim();
         if (rulesQuerqyParser.length() > 0) {
            querqyParser = resourceLoader.newInstance(rulesQuerqyParser, QuerqyParserFactory.class);
         }
      }
      return new querqy.rewrite.commonrules.SimpleCommonRulesRewriterFactory(
            resourceLoader.openResource(rulesResourceName), querqyParser, ignoreCase != null && ignoreCase);
   }

}
