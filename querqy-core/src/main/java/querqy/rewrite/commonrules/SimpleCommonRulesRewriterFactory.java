/**
 * 
 */
package querqy.rewrite.commonrules;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import querqy.model.ExpandedQuery;
import querqy.parser.QuerqyParserFactory;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.commonrules.model.RulesCollection;

/**
 * @author rene
 *
 */
public class SimpleCommonRulesRewriterFactory implements RewriterFactory {

   final RulesCollection rules;

   /**
     * 
     */
   public SimpleCommonRulesRewriterFactory(InputStream is, QuerqyParserFactory querqyParserFactory) throws IOException {
      InputStreamReader reader = new InputStreamReader(is);
      try {
         rules = new SimpleCommonRulesParser(reader, querqyParserFactory).parse();
      } catch (RuleParseException e) {
         throw new IOException(e);
      } finally {
         try {
            reader.close();
         } catch (IOException e) {
            // TODO: log
         }
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * querqy.rewrite.RewriterFactory#createRewriter(querqy.model.ExpandedQuery,
    * java.util.Map)
    */
   @Override
   public QueryRewriter createRewriter(ExpandedQuery input, Map<String, ?> context) {
      return new CommonRulesRewriter(rules);
   }

}
