/**
 *
 */
package querqy.rewrite.commonrules;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import querqy.rewrite.commonrules.model.*;

/**
 * This parser reads a set of rules in the Common Rules format and creates a {@link RulesCollection}.
 *
 * @author rene
 */
public class SimpleCommonRulesParser {

    static final String EMPTY = "".intern();
    static final String QUERQY_NAME_PROPERTY = "querqy_name";

    final BufferedReader reader;
    final QuerqyParserFactory querqyParserFactory;
    int lineNumber = 0;
    final RulesCollectionBuilder builder;
    Input input = null;
    Instructions instructions = null;
    int instructionsCount = 0;


    public SimpleCommonRulesParser(final Reader in, final QuerqyParserFactory querqyParserFactory,
                                   final boolean ignoreCase) {

        this.reader = new BufferedReader(in);
        this.querqyParserFactory = querqyParserFactory;
        this.builder = new TrieMapRulesCollectionBuilder(ignoreCase);
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
            buildLoggerInstruction();
            builder.addRule(input,  instructions);
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
                instructions = new Instructions(instructionsCount++);
            } else if (lineObject instanceof ValidationError) {
                throw new RuleParseException(lineNumber, ((ValidationError) lineObject).getMessage());
            } else if (lineObject instanceof Instruction) {
                instructions.add((Instruction) lineObject);
            } else if (lineObject instanceof Map.Entry) {
                instructions.addProperty((String) ((Map.Entry) lineObject).getKey(),
                         ((Map.Entry) lineObject).getValue());
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
    
    private void buildLoggerInstruction() {
        instructions.add(new DecorateInstruction(QUERQY_NAME_PROPERTY+","+input.getRawInput()));
    }
}
