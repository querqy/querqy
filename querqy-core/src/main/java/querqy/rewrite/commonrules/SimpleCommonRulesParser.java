package querqy.rewrite.commonrules;

import static querqy.rewrite.commonrules.model.Instructions.StandardPropertyNames.ID;
import static querqy.rewrite.commonrules.model.Instructions.StandardPropertyNames.LOG_MESSAGE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntUnaryOperator;

import querqy.rewrite.commonrules.model.*;

/**
 * This parser reads a set of rules in the Common Rules format and creates a {@link RulesCollection}.
 *
 * @author rene
 */
public class SimpleCommonRulesParser {

    private static final String EMPTY = "";

    private final BufferedReader reader;
    private final QuerqyParserFactory querqyParserFactory;
    private int lineNumber = 0;
    private final RulesCollectionBuilder builder;
    private Input input = null;
    private int instructionsCount = 0;
    private List<Instruction> instructionList = null;
    private PropertiesBuilder propertiesBuilder = null;

    private IntUnaryOperator lineNumberMapper = lineNumb -> lineNumb;

    public SimpleCommonRulesParser(final Reader in, final QuerqyParserFactory querqyParserFactory,
                                   final boolean ignoreCase) {
        this(in, querqyParserFactory, new TrieMapRulesCollectionBuilder(ignoreCase));
    }

    public SimpleCommonRulesParser(final Reader in, final QuerqyParserFactory querqyParserFactory,
                                   final RulesCollectionBuilder builder) {
        this.reader = new BufferedReader(in);
        this.querqyParserFactory = querqyParserFactory;
        this.builder = builder;
        this.propertiesBuilder = new PropertiesBuilder();
    }

    public SimpleCommonRulesParser setLineNumberMapper(final IntUnaryOperator lineNumberMapper) {
        this.lineNumberMapper = lineNumberMapper;
        return this;
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

    private void putRule() throws RuleParseException {
        if (input != null) {
            if (instructionList.isEmpty()) {
                throw new RuleParseException(lineNumber, "Instruction expected");
            }

            final int ord = instructionsCount++;

            final String defaultId = String.valueOf(input.getMatchExpression()) + "#" + ord;

            final Object id = propertiesBuilder.addPropertyIfAbsent(ID, defaultId).orElse(defaultId);

            if (id instanceof Collection) {
                throw new RuleParseException(ID + " property must be a single value");
            }

            propertiesBuilder.addPropertyIfAbsent(LOG_MESSAGE, id);

            try {
                builder.addRule(input, new Instructions(ord, id, instructionList, propertiesBuilder.build()));
            } catch (final Exception e) {
                throw new RuleParseException(e);
            }
            input = null;
            instructionList = null;
            propertiesBuilder.reset();
        }
    }

    private void nextLine(final String newLine) throws RuleParseException {
        final String line = stripLine(newLine);
        if (line.length() > 0) {
            Object lineObject = LineParser.parse(line, input, querqyParserFactory);
            if (lineObject instanceof Input) {
                putRule();
                input = (Input) lineObject;
                instructionList = new LinkedList<>();
                propertiesBuilder.reset();
            } else if (lineObject instanceof ValidationError) {
                throw new RuleParseException(lineNumberMapper.applyAsInt(lineNumber), ((ValidationError) lineObject).getMessage());
            } else if (lineObject instanceof Instruction) {
                instructionList.add((Instruction) lineObject);
            } else if (lineObject instanceof String) {
                final Optional<ValidationError> optionalError = propertiesBuilder.nextLine(line);
                if (optionalError.isPresent()) {
                    throw new RuleParseException(lineNumberMapper.applyAsInt(lineNumber), optionalError.map(ValidationError::getMessage).orElse(""));
                }
            }

        }
    }

    String stripLine(String line) {
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
