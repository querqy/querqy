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

import org.junit.Test;
import querqy.model.Clause;
import querqy.model.PhraseQuery;
import querqy.model.StringRawQuery;
import querqy.rewriter.commonrules.QuerqyParserFactory;
import querqy.rewriter.commonrules.WhiteSpaceQuerqyParserFactory;
import querqy.rewriter.commonrules.model.BoostInstruction;
import querqy.rewriter.commonrules.model.DecorateInstruction;
import querqy.rewriter.commonrules.model.DeleteInstruction;
import querqy.rewriter.commonrules.model.FilterInstruction;
import querqy.rewriter.commonrules.model.Instruction;
import querqy.rewriter.commonrules.model.SynonymInstruction;
import querqy.rewriter.commonrules.model.Term;
import querqy.rewrite.RuleParseException;
import querqy.rewriter.commonrules.rules.instruction.skeleton.InstructionSkeleton;
import querqy.rewriter.commonrules.rules.query.QuerqyQueryParser;
import querqy.rewriter.commonrules.rules.query.TermsParser;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static querqy.rewriter.commonrules.rules.instruction.InstructionType.DECORATE;
import static querqy.rewriter.commonrules.rules.instruction.InstructionType.DELETE;
import static querqy.rewriter.commonrules.rules.instruction.InstructionType.DOWN;
import static querqy.rewriter.commonrules.rules.instruction.InstructionType.FILTER;
import static querqy.rewriter.commonrules.rules.instruction.InstructionType.SYNONYM;
import static querqy.rewriter.commonrules.rules.instruction.InstructionType.UP;

public class InstructionParserTest {

    private final QuerqyParserFactory querqyParserFactory = new WhiteSpaceQuerqyParserFactory();

    @Test
    public void testThat_instructionIsParsedProperly_forFilterWithRawQuery() {
        assertThat(parseInstruction(FILTER, "* a:b")).isEqualTo(
                new FilterInstruction(
                        new StringRawQuery(null, "a:b", Clause.Occur.MUST, false)
                ));
    }

    @Test
    public void testThat_instructionIsParsedProperly_forFilterWithQuery() {
        assertThat(parseInstruction(FILTER, " b ")).isEqualTo(
                new FilterInstruction(querqyParserFactory.createParser().parse(" b "))
        );
    }

    @Test
    public void testThat_exceptionIsThrown_forFilterWithEmptyRawQuery() {
        assertThrows(RuleParseException.class,
                () -> parseInstruction(FILTER, "*"));
    }

    @Test
    public void testThat_exceptionIsThrown_forFilterWithBlankRawQuery() {
        assertThrows(RuleParseException.class,
                () -> parseInstruction(FILTER, "*   "));
    }

    @Test
    public void testThat_instructionIsParsedProperly_forUpWithRawQuery() {
        assertThat(parseInstruction(UP, "* a:b")).isEqualTo(
                new BoostInstruction(
                        new StringRawQuery(null, "a:b", Clause.Occur.SHOULD, false),
                        BoostInstruction.BoostDirection.UP,
                        BoostInstruction.BoostMethod.ADDITIVE,
                        1.0f
                ));
    }

    @Test
    public void testThat_instructionIsParsedProperly_forUpWithParameter() {
        assertThat(parseInstruction(UP, "2.5", "b")).isEqualTo(
                new BoostInstruction(
                        querqyParserFactory.createParser().parse(" b "),
                        BoostInstruction.BoostDirection.UP,
                        BoostInstruction.BoostMethod.ADDITIVE,
                        2.5f
                ));
    }

    @Test
    public void testThat_exceptionIsThrown_forUpWithNegativeParameter() {
        assertThrows(RuleParseException.class,
                () -> parseInstruction(UP, "-2.5", "b"));
    }

    @Test
    public void testThat_instructionIsParsedProperly_forDownWithParameter() {
        assertThat(parseInstruction(DOWN, "2.5", "b")).isEqualTo(
                new BoostInstruction(
                        querqyParserFactory.createParser().parse(" b "),
                        BoostInstruction.BoostDirection.DOWN,
                        BoostInstruction.BoostMethod.ADDITIVE,
                        2.5f
                ));
    }

    @Test
    public void testThat_instructionIsParsedProperly_forSynonymWithParameter() {
        assertThat(parseInstruction(SYNONYM, "2.5", "b")).isEqualTo(
                new SynonymInstruction(
                        terms(term("b")),
                        2.5f,
                        null
                ));
    }

    @Test
    public void testThat_instructionIsParsedProperly_forSynonymWithSingleTerm() {
        assertThat(parseInstruction(SYNONYM," b ")).isEqualTo(
                new SynonymInstruction(
                        terms(term("b"))
                ));
    }

    @Test
    public void testThat_instructionIsParsedProperly_forSynonymWithMultipleTerms() {
        assertThat(parseInstruction(SYNONYM," b c")).isEqualTo(
                new SynonymInstruction(
                        terms(term("b"), term("c"))
                ));
    }

    @Test
    public void testThat_instructionIsParsedProperly_forDeleteWithValue() {
        final List<Term> inputTerms = terms(term("a"), term("b"));

        assertThat(parseInstructionWithInputTerms(DELETE, "a", inputTerms)).isEqualTo(
                new DeleteInstruction(
                        terms(term("a"))
                ));
    }

    @Test
    public void testThat_instructionIsParsedProperly_forDeleteWithoutValue() {
        final List<Term> inputTerms = terms(term("a"), term("b"));

        assertThat(parseInstructionWithInputTerms(DELETE, inputTerms)).isEqualTo(
                new DeleteInstruction(
                        terms(term("a"), term("b"))
                ));
    }

    @Test
    public void testThat_exceptionIsThrown_forDeleteTermNotIncludedInInput() {
        final List<Term> inputTerms = terms(term("a"), term("b"));

        assertThrows(RuleParseException.class,
                () -> parseInstructionWithInputTerms(DELETE, "a c", inputTerms));

    }

    @Test
    public void testThat_instructionIsParsedProperly_forDecorationWithoutParam() {
        assertThat(parseInstruction(DECORATE, "dec")).isEqualTo(
                new DecorateInstruction("dec"));
    }

    @Test
    public void testThat_instructionIsParsedProperly_forDecorationWithParam() {
        assertThat(parseInstruction(DECORATE, "par","dec")).isEqualTo(
                new DecorateInstruction("par", "dec"));
    }

    @Test
    public void testThat_instructionIncludesDescription_forSynonymInstruction() {
        final Instruction instruction = parseInstruction(SYNONYM,"b");
        assertThat(instruction.getInstructionDescription()).isNotNull();
        assertThat(instruction.getInstructionDescription().getTypeName()).isEqualTo("synonym");

        assertThat(instruction.getInstructionDescription().getValue()).isPresent();
        assertThat(instruction.getInstructionDescription().getValue().get()).isEqualTo("b");

        assertThat(instruction.getInstructionDescription().getParam()).isNotPresent();
    }

    @Test
    public void testThat_instructionIncludesDescription_forDeleteInstruction() {
        final List<Term> inputTerms = terms(term("a"), term("b"));

        final Instruction instruction = parseInstructionWithInputTerms(DELETE, inputTerms);
        assertThat(instruction.getInstructionDescription()).isNotNull();
        assertThat(instruction.getInstructionDescription().getTypeName()).isEqualTo("delete");

        assertThat(instruction.getInstructionDescription().getValue()).isNotPresent();
        assertThat(instruction.getInstructionDescription().getParam()).isNotPresent();
    }

    @Test
    public void testThat_instructionIncludesDescription_forFilterInstruction() {
        final Instruction instruction = parseInstruction(FILTER, "b");
        assertThat(instruction.getInstructionDescription()).isNotNull();
        assertThat(instruction.getInstructionDescription().getTypeName()).isEqualTo("filter");

        assertThat(instruction.getInstructionDescription().getValue()).isPresent();
        assertThat(instruction.getInstructionDescription().getValue().get()).isEqualTo("b");

        assertThat(instruction.getInstructionDescription().getParam()).isNotPresent();
    }

    @Test
    public void testThat_instructionIncludesDescription_forUpInstruction() {
        final Instruction instruction = parseInstruction(UP, "1.5","b");
        assertThat(instruction.getInstructionDescription()).isNotNull();
        assertThat(instruction.getInstructionDescription().getTypeName()).isEqualTo("up");

        assertThat(instruction.getInstructionDescription().getValue()).isPresent();
        assertThat(instruction.getInstructionDescription().getValue().get()).isEqualTo("b");

        assertThat(instruction.getInstructionDescription().getParam()).isPresent();
        assertThat(instruction.getInstructionDescription().getParam().get()).isEqualTo("1.5");
    }

    @Test
    public void testThat_instructionIncludesDescription_forDownInstruction() {
        final Instruction instruction = parseInstruction(DOWN, "b");
        assertThat(instruction.getInstructionDescription()).isNotNull();
        assertThat(instruction.getInstructionDescription().getTypeName()).isEqualTo("down");
    }

    @Test
    public void testThat_instructionIncludesDescription_forDecorateInstruction() {
        final Instruction instruction = parseInstruction(DECORATE, "b");
        assertThat(instruction.getInstructionDescription()).isNotNull();
        assertThat(instruction.getInstructionDescription().getTypeName()).isEqualTo("decorate");

        assertThat(instruction.getInstructionDescription().getValue()).isPresent();
        assertThat(instruction.getInstructionDescription().getValue().get()).isEqualTo("b");
    }

    private Instruction parseInstructionWithInputTerms(final InstructionType type, final List<Term> inputTerms) {
        return parse(instruction(type, null, null), inputTerms);
    }

    private Instruction parseInstructionWithInputTerms(final InstructionType type,
                                                       final String value,
                                                       final List<Term> inputTerms) {
        return parse(instruction(type, null, value), inputTerms);
    }

    private Instruction parseInstruction(final InstructionType type, final String value) {
        return parse(instruction(type, null, value));
    }

    private Instruction parseInstruction(final InstructionType type, final String parameter, final String value) {
        return parse(instruction(type, parameter, value));
    }

    private InstructionSkeleton instruction(final InstructionType type, final String parameter, final String value) {
        return InstructionSkeleton.builder()
                .type(type)
                .parameter(parameter)
                .value(value)
                .build();
    }

    private Instruction parse(final InstructionSkeleton skeleton) {
        return parse(skeleton, Collections.emptyList());
    }

    private Instruction parse(final InstructionSkeleton skeleton, final List<Term> inputTerms) {
        final InstructionParser parser = parser().with(inputTerms, Collections.singletonList(skeleton));
        return parser.parse().get(0);
    }

    private InstructionParser parser() {
        return InstructionParser.prototypeBuilder()
                .supportedType(FILTER)
                .supportedType(UP)
                .supportedType(DOWN)
                .supportedType(SYNONYM)
                .supportedType(DECORATE)
                .supportedType(DELETE)
                .querqyQueryParser(QuerqyQueryParser.createPrototypeOf(querqyParserFactory))
                .termsParser(TermsParser.createPrototype())
                .boostMethod(BoostInstruction.BoostMethod.ADDITIVE)
                .build();
    }

    // --- isPhraseValue tests ---

    @Test
    public void testIsPhraseValue_simplePhrase() {
        assertThat(InstructionParser.isPhraseValue("\"laptop bag\"")).isTrue();
    }

    @Test
    public void testIsPhraseValue_phraseWithSlop() {
        assertThat(InstructionParser.isPhraseValue("\"laptop bag\"~2")).isTrue();
    }

    @Test
    public void testIsPhraseValue_phraseWithSlopAndSpaces() {
        assertThat(InstructionParser.isPhraseValue("  \"laptop bag\" ~ 3  ")).isTrue();
    }

    @Test
    public void testIsPhraseValue_singleTermPhrase() {
        assertThat(InstructionParser.isPhraseValue("\"laptop\"")).isTrue();
    }

    @Test
    public void testIsPhraseValue_notPhraseNoOpeningQuote() {
        assertThat(InstructionParser.isPhraseValue("laptop bag")).isFalse();
    }

    @Test
    public void testIsPhraseValue_notPhraseNoClosingQuote() {
        assertThat(InstructionParser.isPhraseValue("\"laptop bag")).isFalse();
    }

    @Test
    public void testIsPhraseValue_notPhraseTrailingNonSlop() {
        assertThat(InstructionParser.isPhraseValue("\"laptop bag\"~x")).isFalse();
    }

    @Test
    public void testIsPhraseValue_notPhraseAlphanumericSlop() {
        assertThat(InstructionParser.isPhraseValue("\"laptop bag\"~3a")).isFalse();
    }

    @Test
    public void testIsPhraseValue_notPhraseDecimalSlop() {
        assertThat(InstructionParser.isPhraseValue("\"laptop bag\"~3.1")).isFalse();
    }

    @Test
    public void testIsPhraseValue_emptyString() {
        assertThat(InstructionParser.isPhraseValue("")).isFalse();
    }

    // --- parsePhraseQuery tests ---

    @Test
    public void testParsePhraseQuery_simplePhrase() {
        final PhraseQuery pq = InstructionParser.parsePhraseQuery("\"laptop bag\"", Clause.Occur.SHOULD);
        assertThat(pq.getTerms()).containsExactly("laptop", "bag");
        assertThat(pq.getSlop()).isEqualTo(0);
        assertThat(pq.occur).isEqualTo(Clause.Occur.SHOULD);
    }

    @Test
    public void testParsePhraseQuery_phraseWithSlop() {
        final PhraseQuery pq = InstructionParser.parsePhraseQuery("\"laptop bag\"~2", Clause.Occur.MUST);
        assertThat(pq.getTerms()).containsExactly("laptop", "bag");
        assertThat(pq.getSlop()).isEqualTo(2);
    }

    @Test
    public void testParsePhraseQuery_singleTerm() {
        final PhraseQuery pq = InstructionParser.parsePhraseQuery("\"laptop\"", Clause.Occur.SHOULD);
        assertThat(pq.getTerms()).containsExactly("laptop");
        assertThat(pq.getSlop()).isEqualTo(0);
    }

    @Test
    public void testParsePhraseQuery_emptyPhraseThrows() {
        assertThrows(RuntimeException.class,
                () -> InstructionParser.parsePhraseQuery("\"\"", Clause.Occur.SHOULD));
    }

    // --- integration: UP instruction with phrase ---

    @Test
    public void testUpInstructionWithPhrase() {
        assertThat(parseInstruction(UP, "1.5", "\"laptop bag\"")).isEqualTo(
                new BoostInstruction(
                        new PhraseQuery(null, Clause.Occur.SHOULD, true, Arrays.asList("laptop", "bag"), 0),
                        BoostInstruction.BoostDirection.UP,
                        BoostInstruction.BoostMethod.ADDITIVE,
                        1.5f));
    }

    @Test
    public void testUpInstructionWithPhraseAndSlop() {
        assertThat(parseInstruction(UP, "\"laptop bag\"~3")).isEqualTo(
                new BoostInstruction(
                        new PhraseQuery(null, Clause.Occur.SHOULD, true, Arrays.asList("laptop", "bag"), 3),
                        BoostInstruction.BoostDirection.UP,
                        BoostInstruction.BoostMethod.ADDITIVE,
                        1.0f));
    }

    @Test
    public void testFilterInstructionWithPhrase() {
        assertThat(parseInstruction(FILTER, "\"laptop bag\"")).isEqualTo(
                new FilterInstruction(
                        new PhraseQuery(null, Clause.Occur.MUST, true, Arrays.asList("laptop", "bag"), 0)));
    }

    private Term term(final String value, final String... fields) {
        return new Term(value.toCharArray(), 0, value.length(), Arrays.asList(fields));
    }

    private List<Term> terms(final Term... terms) {
        return Arrays.asList(terms);
    }


}
