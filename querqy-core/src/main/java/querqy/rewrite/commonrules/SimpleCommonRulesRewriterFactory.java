/**
 * 
 */
package querqy.rewrite.commonrules;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

import querqy.model.ExpandedQuery;
import querqy.parser.QuerqyParserFactory;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.commonrules.model.RulesCollection;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class SimpleCommonRulesRewriterFactory implements RewriterFactory {

   final RulesCollection rules;

   /**
    * Creates the rewriter factory.
    * 
    * @param is InputStream to read the rules from using the default character set
    * @param querqyParserFactory
    * @param ignoreCase
    * @throws IOException
    * 
    * @deprecated Use {@link #SimpleCommonRulesRewriterFactory(Reader, QuerqyParserFactory, boolean)}
   */
   public SimpleCommonRulesRewriterFactory(InputStream is, QuerqyParserFactory querqyParserFactory, boolean ignoreCase) throws IOException {
      this(new InputStreamReader(is), querqyParserFactory, ignoreCase);
   }
   
   /**
    * 
    * @param reader
    * @param querqyParserFactory
    * @param ignoreCase
    * @throws IOException
    */
   public SimpleCommonRulesRewriterFactory(Reader reader, QuerqyParserFactory querqyParserFactory, boolean ignoreCase) throws IOException {
       try {
           rules = new SimpleCommonRulesParser(reader, querqyParserFactory, ignoreCase).parse();
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
