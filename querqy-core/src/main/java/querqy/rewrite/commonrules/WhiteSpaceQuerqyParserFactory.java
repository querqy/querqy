/**
 * 
 */
package querqy.rewrite.commonrules;

import querqy.parser.QuerqyParser;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.commonrules.QuerqyParserFactory;

/**
 * This factory creates a {@link WhiteSpaceQuerqyParser}.
 * 
 * @author René Kriegler, @renekrie
 *
 */
public class WhiteSpaceQuerqyParserFactory implements QuerqyParserFactory {

   /*
    * (non-Javadoc)
    * 
    * @see querqy.rewrite.commonrules.QuerqyParserFactory#createParser()
    */
   @Override
   public QuerqyParser createParser() {
      return new WhiteSpaceQuerqyParser();
   }

}
