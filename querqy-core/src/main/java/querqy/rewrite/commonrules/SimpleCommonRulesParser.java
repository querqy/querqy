/**
 *
 */
package querqy.rewrite.commonrules;

import static querqy.rewrite.commonrules.model.Instructions.StandardPropertyNames.ID;
import static querqy.rewrite.commonrules.model.Instructions.StandardPropertyNames.LOG_MESSAGE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    int instructionsCount = 0;
    List<Instruction> instructionList = null;
    Map<String, Object> instructionProperties = null;

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
            if (instructionList.isEmpty()) {
                throw new RuleParseException(lineNumber, "Instruction expected");
            }

            final int ord = instructionsCount++;
            final Object id = instructionProperties
                    .computeIfAbsent(ID, k -> String.valueOf(input.getMatchExpression()) + "#" + ord);

            instructionProperties.putIfAbsent(LOG_MESSAGE, id);

            builder.addRule(input, new Instructions(ord, id, instructionList, instructionProperties));
            input = null;
            instructionProperties = null;
            instructionList = null;
        }
    }

    public void nextLine(String line) throws RuleParseException {
        line = stripLine(line);
        if (line.length() > 0) {
            Object lineObject = LineParser.parse(line, input, querqyParserFactory);
            if (lineObject instanceof Input) {
                putRule();
                input = (Input) lineObject;
                instructionList = new LinkedList<>();
                instructionProperties = new HashMap<>();
            } else if (lineObject instanceof ValidationError) {
                throw new RuleParseException(lineNumber, ((ValidationError) lineObject).getMessage());
            } else if (lineObject instanceof Instruction) {
                instructionList.add((Instruction) lineObject);
            } else if (lineObject instanceof Map.Entry) {
                instructionProperties.put((String) ((Map.Entry) lineObject).getKey(),
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
    
}
