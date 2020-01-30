package querqy.rewrite.commonrules;

import querqy.parser.FieldAwareWhiteSpaceQuerqyParser;
import querqy.parser.QuerqyParser;

/**
 * This factory creates a {@link FieldAwareWhiteSpaceQuerqyParser}.
 *
 * @author René Kriegler, @renekrie
 *
 */
public class FieldAwareWhiteSpaceQuerqyParserFactory implements QuerqyParserFactory {

    @Override
    public QuerqyParser createParser() {
        return new FieldAwareWhiteSpaceQuerqyParser();
    }
}
