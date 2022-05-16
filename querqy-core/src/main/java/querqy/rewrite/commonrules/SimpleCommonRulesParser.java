package querqy.rewrite.commonrules;

import querqy.model.Input;
import querqy.rewrite.commonrules.model.Instruction;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.RulesCollection;
import querqy.rewrite.commonrules.model.RulesCollectionBuilder;
import querqy.rewrite.commonrules.model.TrieMapRulesCollectionBuilder;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputLiteral;
import querqy.rewrite.commonrules.select.booleaninput.BooleanInputParser;

import static querqy.rewrite.commonrules.EscapeUtil.indexOfComment;
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

    static final char COMMENT_START = '#';

    private static final String EMPTY = "";

    private final BufferedReader reader;
    private final QuerqyParserFactory querqyParserFactory;
    private int lineNumber = 0;
    private final RulesCollectionBuilder builder;
    private final BooleanInputParser booleanInputParser;
    private Input inputPattern = null;
    private int instructionsCount = 0;
    private List<Instruction> instructionList = null;
    private final PropertiesBuilder propertiesBuilder;
    private final boolean multiplicativeBoosts;

    private final Set<Object> seenInstructionIds = new HashSet<>();
    private IntUnaryOperator lineNumberMapper = lineNumb -> lineNumb;

    public SimpleCommonRulesParser(final Reader in, final boolean allowBooleanInput,
                                   final QuerqyParserFactory querqyParserFactory,
                                   final boolean ignoreCase,
                                   final boolean multiplicativeBoosts) {
        this(in, allowBooleanInput, querqyParserFactory, new TrieMapRulesCollectionBuilder(ignoreCase), multiplicativeBoosts);
    }

    public SimpleCommonRulesParser(final Reader in, final boolean allowBooleanInput,
                                   final QuerqyParserFactory querqyParserFactory,
                                   final RulesCollectionBuilder builder,
                                   final boolean multiplicativeBoosts) {
        this.reader = new BufferedReader(in);
        this.querqyParserFactory = querqyParserFactory;
        this.builder = builder;
        this.propertiesBuilder = new PropertiesBuilder();
        this.booleanInputParser = allowBooleanInput ? new BooleanInputParser() : null;
        this.multiplicativeBoosts = multiplicativeBoosts;
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
            if (booleanInputParser != null) {
                addLiterals();
            }
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
        for (final BooleanInputLiteral literal : booleanInputParser.getLiteralRegister().values()) {
            final Object parsingResult = LineParser.parseInput(String.join(" ", literal.getTerms()));

            if (parsingResult instanceof Input.SimpleInput) {
                builder.addRule((Input.SimpleInput) parsingResult, literal);

            } else if (parsingResult instanceof ValidationError) {
                throw new RuleParseException(((ValidationError) parsingResult).getMessage());
            } else {
                throw new RuleParseException(String.format("Something unexpected happened parsing boolean input %s",
                        String.join(" ", literal.getTerms())));
            }
        }
    }

    private void putRule() throws RuleParseException {
        if (inputPattern != null) {
            if (instructionList.isEmpty()) {
                throw new RuleParseException(lineNumber, "Instruction expected");
            }

            final int ord = instructionsCount++;

            final String defaultId = inputPattern.getIdPrefix() + "#" + ord;

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

                inputPattern.applyInstructions(instructions, builder);

            } catch (final Exception e) {
                throw new RuleParseException(e);
            }
            inputPattern = null;
            instructionList = null;
            propertiesBuilder.reset();
        }
    }

    private void nextLine(final String newLine) throws RuleParseException {
        final String line = stripLine(newLine);
        if (line.length() > 0) {
            final Object lineObject = LineParser.parse(line, inputPattern, querqyParserFactory, multiplicativeBoosts);

            if (lineObject instanceof InputString) {

                final Object patternObject = Input.fromString(((InputString) lineObject).value,
                        booleanInputParser);

                if (patternObject instanceof ValidationError) {

                    throw new RuleParseException(lineNumberMapper.applyAsInt(lineNumber),
                            ((ValidationError) patternObject).getMessage());

                } else if (patternObject instanceof Input) {

                    putRule();
                    inputPattern = (Input) patternObject;
                    instructionList = new LinkedList<>();
                    propertiesBuilder.reset();

                }
            } else if (lineObject instanceof ValidationError) {
                throw new RuleParseException(lineNumberMapper.applyAsInt(lineNumber),
                        ((ValidationError) lineObject).getMessage());
            } else if (lineObject instanceof Instruction) {
                instructionList.add((Instruction) lineObject);
            } else if (lineObject instanceof String) {
                final Optional<ValidationError> optionalError = propertiesBuilder.nextLine(line);
                if (optionalError.isPresent()) {
                    throw new RuleParseException(lineNumberMapper.applyAsInt(lineNumber),
                            optionalError.map(ValidationError::getMessage).orElse(""));
                }
            }

        }
    }

    String stripLine(String line) {
        line = line.trim();
        if (line.length() > 0) {
            int pos = indexOfComment(line);
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
