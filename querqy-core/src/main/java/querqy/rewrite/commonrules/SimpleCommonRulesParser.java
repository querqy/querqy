/**
 *
 */
package querqy.rewrite.commonrules;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import querqy.rewrite.commonrules.model.*;
import querqy.utils.Constants;

/**
 * This parser reads a set of rules in the Common Rules format and creates a {@link RulesCollection}.
 *
 * @author rene
 */
public class SimpleCommonRulesParser {

    static final String EMPTY = "".intern();
    static final String ARROW_OP = "=>";

    final BufferedReader reader;
    final QuerqyParserFactory querqyParserFactory;
    int lineNumber = 0;
    final RulesCollectionBuilder builder;
    Input input = null;
    Instructions instructions = null;
    Map<String, String> propertyMap = null;

    @Deprecated
    public SimpleCommonRulesParser(Reader in, QuerqyParserFactory querqyParserFactory, boolean ignoreCase) {
        this(in, querqyParserFactory, ignoreCase, Constants.DEFAULT_RULES_MAP);
    }

    public SimpleCommonRulesParser(Reader in, QuerqyParserFactory querqyParserFactory, boolean ignoreCase, String rulesMapType) {

        this.reader = new BufferedReader(in);
        this.querqyParserFactory = querqyParserFactory;
        switch (rulesMapType) {
            case Constants.PROPERTY_RULES_MAP:
                builder = new TrieMapRulesPropertiesCollectionBuilder(ignoreCase);
                break;
            case Constants.DEFAULT_RULES_MAP:
            default:
                builder = new TrieMapRulesCollectionBuilder(ignoreCase);
        }
    }

    public RulesCollection parse() throws IOException, RuleParseException {
        try {
            lineNumber = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                nextLine(line);
            }
            putRule();
            return builder.build();
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
                // TODO: log
            }
        }
    }

    public void putRule() throws RuleParseException {
        if (input != null) {
            if (instructions.isEmpty()) {
                throw new RuleParseException(lineNumber, "Instruction expected");
            }
            builder.addRule(input,  new Properties(instructions, propertyMap));
            input = null;
            //  instructions = new Instructions();
        }
    }

    public void nextLine(String line) throws RuleParseException {
        line = stripLine(line);
        if (line.length() > 0) {
            Object lineObject = LineParser.parse(line, input, querqyParserFactory);
            if (lineObject instanceof Input) {
                putRule();
                input = (Input) lineObject;
                instructions = new Instructions();
                propertyMap = new HashMap<>();
            } else if (lineObject instanceof ValidationError) {
                throw new RuleParseException(lineNumber, ((ValidationError) lineObject).getMessage());
            } else if (lineObject instanceof Instruction) {
                instructions.add((Instruction) lineObject);
            } else if (lineObject instanceof Map.Entry) {
                propertyMap.put((String) ((Map.Entry) lineObject).getKey(),
                        (String) ((Map.Entry) lineObject).getValue());
            }

        }
    }

    public String stripLine(String line) {
        line = line.trim();
        if (line.length() > 0) {
            int pos = line.indexOf('#');
            if (pos == 0) {
                return EMPTY;
            }
            if (pos > 0) {
                line = line.substring(0, pos);
            }
        }
        return line;
    }

}
