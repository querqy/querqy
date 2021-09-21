package querqy.rewrite.rules.instruction.skeleton;

import org.junit.Test;
import querqy.rewrite.rules.RuleParseException;
import querqy.rewrite.rules.instruction.skeleton.InstructionSkeleton;
import querqy.rewrite.rules.instruction.InstructionType;
import querqy.rewrite.rules.instruction.skeleton.InstructionSkeletonParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

public class InstructionSkeletonParserTest {
    
    @Test
    public void testThat_instructionIsParsed_forDefinitionWithoutValue() {
        assertThat(
                parse("delete")).isEqualTo(
                        instruction(InstructionType.DELETE, null));

        assertThat(
                parse("delete:  ")).isEqualTo(
                        instruction(InstructionType.DELETE, null));
    }

    @Test
    public void testThat_instructionIsProperlyParsed_forDefinitionWithoutParameters() {
        assertThat(
                parse("up: val")).isEqualTo(
                        instruction(InstructionType.UP, "val"));
    }

    @Test
    public void testThat_instructionIsProperlyParsed_forDefinitionWithParameters() {
        assertThat(
                parse("up(1.0): val")).isEqualTo(
                        instruction(InstructionType.UP, "1.0", "val"));
    }

    @Test
    public void testThat_instructionIsProperlyParsed_forDefinitionWithParametersAndManyWhitespaces() {
        assertThat(
                parse("   up  (    \t 1.0  )  :  val  ")).isEqualTo(
                        instruction(InstructionType.UP, "1.0", "val"));
    }

    @Test
    public void testThat_exceptionIsThrown_forInvalidParameterDefinitionWithMissingClosingBracket() {
        assertIsNotParsable("up(1.0: val");
    }

    @Test
    public void testThat_exceptionIsThrown_forInvalidParameterDefinitionWithMissingOpeningBracket() {
        assertIsNotParsable("up1.0): val");
    }

    @Test
    public void testThat_exceptionIsThrown_forMissingColon() {
        assertIsNotParsable("up(1.0)val");
    }

    @Test
    public void testThat_exceptionIsThrown_forUnknownType() {
        assertThrows(RuleParseException.class,
                () -> parse("uup(1.0): val"));
    }

    private InstructionSkeleton parse(final String instructionDefinition) {
        final InstructionSkeletonParser parser = InstructionSkeletonParser.create();
        parser.setContent(instructionDefinition);
        parser.isParsable();
        parser.parse();
        return parser.finish();
    }

    private void assertIsNotParsable(final String instructionDefinition) {
        final InstructionSkeletonParser parser = InstructionSkeletonParser.create();
        parser.setContent(instructionDefinition);
        assertFalse(parser.isParsable());
    }

    private InstructionSkeleton instruction(final InstructionType type,
                                            final String value) {
        return instruction(type, null, value);
    }

    private InstructionSkeleton instruction(final InstructionType type,
                                            final String parameter,
                                            final String value) {
        return InstructionSkeleton.builder()
                .type(type)
                .parameter(parameter)
                .value(value)
                .build();
    }



}
