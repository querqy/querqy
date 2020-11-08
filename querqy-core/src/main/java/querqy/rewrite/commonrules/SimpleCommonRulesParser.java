package querqy.rewrite.commonrules;

import querqy.rewrite.commonrules.model.Input;
import querqy.rewrite.commonrules.model.Instruction;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.RulesCollection;
import querqy.rewrite.commonrules.model.RulesCollectionBuilder;
import querqy.rewrite.commonrules.model.TrieMapRulesCollectionBuilder;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputLiteral;
import querqy.rewrite.commonrules.select.booleaninput.BooleanInputParser;
import querqy.rewrite.commonrules.select.booleaninput.BooleanInputParser.BooleanInputString;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInput.BooleanInputBuilder;

import static querqy.rewrite.commonrules.model.Instructions.StandardPropertyNames.ID;
import static querqy.rewrite.commonrules.model.Instructions.StandardPropertyNames.LOG_MESSAGE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntUnaryOperator;


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
    private final BooleanInputParser booleanInputParser;
    private Input input = null;
    private BooleanInputBuilder booleanInputBuilder = null;
    private int instructionsCount = 0;
    private List<Instruction> instructionList = null;
    private PropertiesBuilder propertiesBuilder = null;

    private final Set<Object> seenInstructionIds = new HashSet<>();
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
        this.booleanInputParser = new BooleanInputParser();
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
            addLiterals();
            return builder.build();
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
                // TODO: log
            }
        }
    }

    private void addLiterals() throws RuleParseException {
        for (final BooleanInputLiteral literal : this.booleanInputParser.getLiteralRegister().values()) {
            final Object parsingResult = LineParser.parseInput(String.join(" ", literal.getTerms()));

            if (parsingResult instanceof Input) {
                builder.addRule((Input) parsingResult, literal);

            } else if (parsingResult instanceof ValidationError) {
                throw new RuleParseException(lineNumber, ((ValidationError) parsingResult).getMessage());
            } else {
                throw new RuleParseException(String.format(
                        "Something unexpected happened parsing boolean input %s",
                        String.join(" ", literal.getTerms())));
            }
        }
    }

    private void putRule() throws RuleParseException {
        if (input != null || booleanInputBuilder != null) {
            if (instructionList.isEmpty()) {
                throw new RuleParseException(lineNumber, "Instruction expected");
            }

            final int ord = instructionsCount++;

            final String defaultId = input != null
                    ? String.valueOf(input.getMatchExpression()) + "#" + ord
                    : booleanInputBuilder.getBooleanInputString() + "#" + ord;

            final Object id = propertiesBuilder.addPropertyIfAbsent(ID, defaultId).orElse(defaultId);

            if (id instanceof Collection) {
                throw new RuleParseException(ID + " property must be a single value");
            }

            propertiesBuilder.addPropertyIfAbsent(LOG_MESSAGE, id);

            try {
                final Instructions instructions = new Instructions(ord, id, instructionList, propertiesBuilder.build());

                if (seenInstructionIds.contains(instructions.getId())) {
                    throw new IllegalStateException("Duplicate instructions ID " + instructions.getId());
                }
                seenInstructionIds.add(instructions.getId());

                if (booleanInputBuilder != null) {
                    booleanInputBuilder.linkToInstructions(instructions).build();
//                    builder.addRule(new Rule(booleanInputBuilder.build(), instructions));
                } else {
                    builder.addRule(input, instructions);
                }

            } catch (final Exception e) {
                throw new RuleParseException(e);
            }
            input = null;
            booleanInputBuilder = null;
            instructionList = null;
            propertiesBuilder.reset();
        }
    }

    private void nextLine(final String newLine) throws RuleParseException {
        final String line = stripLine(newLine);
        if (line.length() > 0) {
            Object lineObject = LineParser.parse(line, input, booleanInputBuilder, querqyParserFactory);
            if (lineObject instanceof Input) {
                putRule();
                input = (Input) lineObject;
                instructionList = new LinkedList<>();
                propertiesBuilder.reset();

            } else if (lineObject instanceof BooleanInputString) {
                putRule();
                booleanInputBuilder = booleanInputParser.parseBooleanInput((BooleanInputString) lineObject);

//                for (final BooleanInputLiteral literal : booleanInputBuilder.getLiterals()) {
//                    if (!literal.hasInput()) {
//                        final Object parsingResult = LineParser.parseInput(String.join(" ", literal.getTerms()));
//
//                        if (parsingResult instanceof Input) {
//                            literal.setInput((Input) parsingResult);
//
//                        } else if (parsingResult instanceof ValidationError) {
//                            throw new RuleParseException(lineNumber, ((ValidationError) parsingResult).getMessage());
//                        } else {
//                            throw new RuleParseException(String.format("Something unexpected happened parsing line %s", line));
//                        }
//                    }
//                }

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
