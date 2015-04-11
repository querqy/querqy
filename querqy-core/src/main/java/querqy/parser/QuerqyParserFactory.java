package querqy.parser;

/**
 * The interface for factories that provide a {@link QuerqyParser}. 
 * 
 * @author René Kriegler, @renekrie
 *
 */
public interface QuerqyParserFactory {

   QuerqyParser createParser();
}
