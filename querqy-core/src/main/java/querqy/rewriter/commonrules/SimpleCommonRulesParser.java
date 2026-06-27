/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014 Querqy Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package querqy.rewriter.commonrules;

import querqy.model.Input;
import querqy.rewriter.commonrules.model.BoostInstruction;
import querqy.rewriter.commonrules.model.Instruction;
import querqy.rewriter.commonrules.model.Instructions;
import querqy.rewriter.commonrules.model.RulesCollection;
import querqy.rewriter.commonrules.model.RulesCollectionBuilder;
import querqy.rewriter.commonrules.model.TrieMapRulesCollectionBuilder;
import querqy.rewriter.commonrules.select.booleaninput.model.BooleanInputLiteral;
import querqy.rewriter.commonrules.select.booleaninput.BooleanInputParser;

import static querqy.rewriter.commonrules.EscapeUtil.indexOfComment;
import static querqy.rewriter.commonrules.model.Instructions.StandardPropertyNames.ID;
import static querqy.rewriter.commonrules.model.Instructions.StandardPropertyNames.LOG_MESSAGE;

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
    private final BoostInstruction.BoostMethod boostMethod;

    private final Set<Object> seenInstructionIds = new HashSet<>();
    private IntUnaryOperator lineNumberMapper = lineNumb -> lineNumb;

    public SimpleCommonRulesParser(final Reader in, final boolean allowBooleanInput,
                                   final QuerqyParserFactory querqyParserFactory,
                                   final boolean ignoreCase,
                                   final BoostInstruction.BoostMethod boostMethod) {
        this(in, allowBooleanInput, querqyParserFactory, new TrieMapRulesCollectionBuilder(ignoreCase), boostMethod);
    }

    public SimpleCommonRulesParser(final Reader in, final boolean allowBooleanInput,
                                   final QuerqyParserFactory querqyParserFactory,
                                   final RulesCollectionBuilder builder,
                                   final BoostInstruction.BoostMethod boostMethod) {
        this.reader = new BufferedReader(in);
        this.querqyParserFactory = querqyParserFactory;
        this.builder = builder;
        this.propertiesBuilder = new PropertiesBuilder();
        this.booleanInputParser = allowBooleanInput ? new BooleanInputParser() : null;
        this.boostMethod = boostMethod;
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
            final Object lineObject = LineParser.parse(line, inputPattern, querqyParserFactory, boostMethod);

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
