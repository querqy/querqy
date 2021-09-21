package querqy.rewrite.rules.instruction;

import org.junit.Test;
import querqy.model.Clause;
import querqy.model.StringRawQuery;
import querqy.rewrite.commonrules.QuerqyParserFactory;
import querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory;
import querqy.rewrite.commonrules.model.BoostInstruction;
import querqy.rewrite.commonrules.model.DecorateInstruction;
import querqy.rewrite.commonrules.model.DeleteInstruction;
import querqy.rewrite.commonrules.model.FilterInstruction;
import querqy.rewrite.commonrules.model.Instruction;
import querqy.rewrite.commonrules.model.SynonymInstruction;
import querqy.rewrite.commonrules.model.Term;
import querqy.rewrite.rules.RuleParseException;
import querqy.rewrite.rules.instruction.skeleton.InstructionSkeleton;
import querqy.rewrite.rules.query.QuerqyQueryParser;
import querqy.rewrite.rules.query.TermsParser;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static querqy.rewrite.rules.instruction.InstructionType.DECORATE;
import static querqy.rewrite.rules.instruction.InstructionType.DELETE;
import static querqy.rewrite.rules.instruction.InstructionType.DOWN;
import static querqy.rewrite.rules.instruction.InstructionType.FILTER;
import static querqy.rewrite.rules.instruction.InstructionType.SYNONYM;
import static querqy.rewrite.rules.instruction.InstructionType.UP;

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
    public void testThat_instructionIsParsedProperly_forUpWithRawQuery() {
        assertThat(parseInstruction(UP, "* a:b")).isEqualTo(
                new BoostInstruction(
                        new StringRawQuery(null, "a:b", Clause.Occur.SHOULD, false),
                        BoostInstruction.BoostDirection.UP,
                        1.0f
                ));
    }

    @Test
    public void testThat_instructionIsParsedProperly_forUpWithParameter() {
        assertThat(parseInstruction(UP, "2.5", "b")).isEqualTo(
                new BoostInstruction(
                        querqyParserFactory.createParser().parse(" b "),
                        BoostInstruction.BoostDirection.UP,
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
                        2.5f
                ));
    }

    @Test
    public void testThat_instructionIsParsedProperly_forSynonymWithParameter() {
        assertThat(parseInstruction(SYNONYM, "2.5", "b")).isEqualTo(
                new SynonymInstruction(
                        terms(term("b")),
                        2.5f
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
                .build();
    }

    private Term term(final String value, final String... fields) {
        return new Term(value.toCharArray(), 0, value.length(), Arrays.asList(fields));
    }

    private List<Term> terms(final Term... terms) {
        return Arrays.asList(terms);
    }


}
