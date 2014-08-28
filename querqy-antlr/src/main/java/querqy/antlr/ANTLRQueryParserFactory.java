/**
 * 
 */
package querqy.antlr;

import querqy.parser.QuerqyParser;
import querqy.parser.QuerqyParserFactory;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class ANTLRQueryParserFactory implements QuerqyParserFactory {

   /*
    * (non-Javadoc)
    * 
    * @see querqy.parser.QuerqyParserFactory#createParser()
    */
   @Override
   public QuerqyParser createParser() {
      return new ANTLRQueryParser();
   }

}
