package querqy.rewrite.commonrules;

import querqy.parser.QuerqyParser;

/**
 * The interface for factories that provide a {@link QuerqyParser}.
 * 
 * @author René Kriegler, @renekrie
 *
 */
public interface QuerqyParserFactory {

   QuerqyParser createParser();
}
