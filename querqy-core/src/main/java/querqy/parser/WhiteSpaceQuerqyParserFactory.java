/**
 * 
 */
package querqy.parser;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class WhiteSpaceQuerqyParserFactory implements QuerqyParserFactory {

	
	/* (non-Javadoc)
	 * @see querqy.parser.QuerqyParserFactory#createParser()
	 */
	@Override
	public QuerqyParser createParser() {
		return new WhiteSpaceQuerqyParser();
	}

}
