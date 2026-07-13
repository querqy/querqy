/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2021 Querqy Contributors
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
package querqy.rewriter.commonrules.rules.instruction;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import querqy.model.Clause;
import querqy.model.PhraseQuery;
import querqy.model.QuerqyQuery;
import querqy.rewriter.commonrules.model.BoostInstruction;
import querqy.rewriter.commonrules.model.DecorateInstruction;
import querqy.rewriter.commonrules.model.DeleteInstruction;
import querqy.rewriter.commonrules.model.FilterInstruction;
import querqy.rewriter.commonrules.model.Instruction;
import querqy.rewriter.commonrules.model.InstructionDescription;
import querqy.rewriter.commonrules.model.SynonymInstruction;
import querqy.rewriter.commonrules.model.Term;
import querqy.rewriter.commonrules.rules.RuleParseException;
import querqy.rewriter.commonrules.rules.instruction.skeleton.InstructionSkeleton;
import querqy.rewriter.commonrules.rules.query.QuerqyQueryParser;
import querqy.rewriter.commonrules.rules.query.TermsParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
public class InstructionParser {

    private final List<Instruction> instructions = new ArrayList<>();

    private final Set<InstructionType> supportedTypes;
    private final QuerqyQueryParser querqyQueryParser;
    private final TermsParser termsParser;
    private final BoostInstruction.BoostMethod boostMethod;

    private final List<Term> inputTerms;
    private final List<InstructionSkeleton> skeletons;

    private InstructionSkeleton skeleton;

    @Builder(builderClassName = "PrototypeBuilder", builderMethodName = "prototypeBuilder")
    protected InstructionParser(@Singular final Set<InstructionType> supportedTypes,
                                final QuerqyQueryParser querqyQueryParser,
                                final TermsParser termsParser,
                                final BoostInstruction.BoostMethod boostMethod) {
        this.supportedTypes = supportedTypes;
        this.querqyQueryParser = querqyQueryParser;
        this.termsParser = termsParser;
        this.boostMethod = boostMethod;
        this.inputTerms = Collections.emptyList();
        this.skeletons = Collections.emptyList();
    }

    public InstructionParser with(final List<Term> inputTerms, final List<InstructionSkeleton> skeletons) {
        return of(supportedTypes, querqyQueryParser, termsParser, boostMethod, inputTerms, skeletons);
    }

    public List<Instruction> parse() {
        for (final InstructionSkeleton nextSkeleton : skeletons) {
            skeleton = nextSkeleton;
            parseSkeleton();
        }

        return instructions;
    }

    private void parseSkeleton() {
        assertThatTypeIsSupported(skeleton.getType());

        switch (skeleton.getType()) {
            case SYNONYM:
                parseAsSynonym(); break;

            case UP:
                parseAsBoost(BoostInstruction.BoostDirection.UP); break;

            case DOWN:
                parseAsBoost(BoostInstruction.BoostDirection.DOWN); break;

            case FILTER:
                parseAsFilter(); break;

            case DELETE:
                parseAsDelete(); break;

            case REPLACE:
                parseAsReplace(); break;

            case DECORATE:
                parseAsDecorate(); break;

            default:
                throw new RuleParseException("No parsing implemented for the given instruction type " + skeleton.getType());

        }
    }

    public void assertThatTypeIsSupported(final InstructionType type) {
        if (!supportedTypes.contains(type)) {
            throw new RuleParseException(
                    String.format("Instruction of type %s is not supported", skeleton.getType().name()));
        }
    }

    private void parseAsSynonym() {
        final float param = getParamAsFloat();
        final String value = getValueOrElseThrow();
        final List<Term> terms = termsParser.with(value).parse();

        instructions.add(new SynonymInstruction(terms, param, createInstructionDescription()));
    }

    private void parseAsBoost(final BoostInstruction.BoostDirection direction) {
        final float param = getParamAsFloat();
        final String value = getValueOrElseThrow();
        final QuerqyQuery<?> querqyQuery = isPhraseValue(value)
                ? parsePhraseQuery(value, Clause.Occur.SHOULD)
                : querqyQueryParser.with(value, Clause.Occur.SHOULD).parse();
        instructions.add(new BoostInstruction(querqyQuery, direction, boostMethod, param, createInstructionDescription()));
    }

    private void parseAsFilter() {
        assertThatParamIsNotSet();

        final String value = getValueOrElseThrow();
        final QuerqyQuery<?> querqyQuery = isPhraseValue(value)
                ? parsePhraseQuery(value, Clause.Occur.MUST)
                : querqyQueryParser.with(value, Clause.Occur.MUST).parse();

        instructions.add(new FilterInstruction(querqyQuery, createInstructionDescription()));
    }

    /**
     * Returns true if {@code value} represents a phrase: starts with {@code "} and
     * has a closing {@code "} optionally followed by a slop spec ({@code ~N}).
     * Examples: {@code "laptop bag"}, {@code "laptop bag"~2}
     */
    public static boolean isPhraseValue(final String value) {
        final String trimmed = value.trim();
        if (trimmed.length() < 2 || trimmed.charAt(0) != '"') {
            return false;
        }
        final int closingQuote = trimmed.indexOf('"', 1);
        if (closingQuote < 0) {
            return false;
        }
        final String afterClosingQuote = trimmed.substring(closingQuote + 1).trim();
        if (afterClosingQuote.isEmpty()) {
            return true;
        }
        if (afterClosingQuote.startsWith("~")) {
            final String slopPart = afterClosingQuote.substring(1).trim();
            return slopPart.matches("\\d+");
        }
        return false;
    }

    public static PhraseQuery parsePhraseQuery(final String value, final Clause.Occur occur) {
        final String trimmed = value.trim();
        // find the closing quote
        final int closingQuote = trimmed.indexOf('"', 1);
        String phraseContent = trimmed.substring(1, closingQuote).trim();

        int slop = 0;
        final String afterClosingQuote = trimmed.substring(closingQuote + 1).trim();
        if (afterClosingQuote.startsWith("~")) {
            try {
                slop = Integer.parseInt(afterClosingQuote.substring(1).trim());
            } catch (final NumberFormatException e) {
                throw new RuleParseException("Invalid slop value in phrase query: " + value);
            }
            if (slop < 0) {
                throw new RuleParseException("Slop must not be negative: " + value);
            }
        }

        final String[] parts = phraseContent.split("\\s+");
        final List<String> terms = Arrays.asList(parts);
        if (terms.isEmpty() || (terms.size() == 1 && terms.get(0).isEmpty())) {
            throw new RuleParseException("Phrase query must contain at least one term: " + value);
        }
        return new PhraseQuery(null, occur, true, terms, slop);
    }

    private void parseAsDelete() {
        assertThatParamIsNotSet();
        final Optional<String> optionalValue = skeleton.getValue();

        if (optionalValue.isPresent()) {
            parseAsDeleteWithValue(optionalValue.get());

        } else {
            instructions.add(new DeleteInstruction(inputTerms, createInstructionDescription()));
        }
    }

    private void parseAsDeleteWithValue(final String value) {
        final List<Term> deleteTerms = termsParser.with(value).parse();
        validateDeleteTerms(deleteTerms);
        instructions.add(new DeleteInstruction(deleteTerms, createInstructionDescription()));
    }

    private void validateDeleteTerms(final List<Term> deleteTerms) {
        for (final Term deleteTerm : deleteTerms) {
            if (deleteTerm.findFirstMatch(inputTerms) == null) {
                throw new RuleParseException("Condition doesn't contain the term to delete: " + deleteTerm);
            }
        }
    }

    private void parseAsReplace() {
        throw new UnsupportedOperationException("Replace instructions cannot be parsed so far");
    }

    private void parseAsDecorate() {
        final String value = getValueOrElseThrow();
        final Optional<String> optionalParam = skeleton.getParameter();

        if (optionalParam.isPresent()) {
            instructions.add(new DecorateInstruction(optionalParam.get(), value, createInstructionDescription()));

        } else {
            instructions.add(new DecorateInstruction(null, value, createInstructionDescription()));
        }
    }

    private float getParamAsFloat() {
        try {
            final float param = skeleton.getParameter()
                    .map(Float::parseFloat)
                    .orElse(1.0f);

            assertThatParamAsFloatIsNotNegative(param);
            return param;

        } catch (final NumberFormatException e) {
            throw new RuleParseException(
                    String.format("Instruction of type %s expects a float or nothing as parameter",
                            skeleton.getType().name()));
        }
    }

    private void assertThatParamAsFloatIsNotNegative(final float param) {
        if (param < 0) {
            throw new RuleParseException("Parameter must not be negative: " + param);
        }
    }

    private void assertThatParamIsNotSet() {
        if (skeleton.getParameter().isPresent()) {
            throw new RuleParseException(
                    String.format("Instruction of type %s does not support parameters", skeleton.getType().name()));
        }
    }

    private String getValueOrElseThrow() {
        return skeleton.getValue().orElseThrow(() ->
                new RuleParseException(
                        String.format("Instruction of type %s requires a value", skeleton.getType().name())));
    }

    private InstructionDescription createInstructionDescription() {
        final InstructionDescription.Builder builder = InstructionDescription.builder();

        builder.typeName(skeleton.getType().getTypeName());
        skeleton.getParameter().ifPresent(builder::param);
        skeleton.getValue().ifPresent(builder::value);

        return builder.build();
    }

}
