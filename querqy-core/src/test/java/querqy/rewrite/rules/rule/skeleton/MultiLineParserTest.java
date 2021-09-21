package querqy.rewrite.rules.rule.skeleton;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import querqy.rewrite.rules.RuleParseException;
import querqy.rewrite.rules.input.skeleton.InputSkeletonParser;
import querqy.rewrite.rules.instruction.skeleton.InstructionSkeleton;
import querqy.rewrite.rules.instruction.InstructionType;
import querqy.rewrite.rules.instruction.skeleton.InstructionSkeletonParser;
import querqy.rewrite.rules.property.skeleton.PropertySkeletonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MultiLineParserTest {

    @Test
    public void testThat_emptyListIsReturned_forFinishingWithoutParsing() {
        final MultiLineParser parser = parser();
        assertThat(parser.finish()).isEmpty();
    }

    @Test
    public void testThat_parserThrowsException_forNonParsableLine() {
        final MultiLineParser parser = parser();
        parser.parse(InputSkeletonParser.toTextDefinition("input"));

        assertThrows(RuleParseException.class, () -> parser.parse("non-parsable"));
    }

    @Test
    public void testThat_parserParsesInputProperly_forInputAndInstructions() {
        final RuleSkeleton ruleSkeleton = rule(
                "notebook",
                synonymInstruction("notebook"),
                upInstruction("notebook", "1.0"));

        final String rulesAsText = asText(ruleSkeleton);
        assertThat(parse(rulesAsText)).isEqualTo(Collections.singletonList(ruleSkeleton));
    }

    @Test
    public void testThat_parserParsesInputProperly_forRuleWithMultiLineProperties() {
        final RuleSkeleton ruleSkeleton = rule(
                "notebook",
                synonymInstruction("notebook"),
                property("_id", "abc"),
                property("priority", 2)
        );

        final String rulesAsText = asText(ruleSkeleton);
        assertThat(parse(rulesAsText)).isEqualTo(Collections.singletonList(ruleSkeleton));
    }

    private RuleSkeleton rule(final String input, final InstructionSkeleton... instructionSkeletons) {
        return RuleSkeleton.builder()
                .inputSkeleton(input)
                .instructionSkeletons(Arrays.asList(instructionSkeletons))
                .build();
    }

    private RuleSkeleton rule(final String input,
                              final InstructionSkeleton instructionSkeleton,
                              final Property... properties) {

        final Map<String, Object> propertiesAsMap = Arrays.stream(properties).collect(
                Collectors.toMap(Property::getKey, Property::getValue));

        return RuleSkeleton.builder()
                .inputSkeleton(input)
                .instructionSkeletons(Collections.singletonList(instructionSkeleton))
                .properties(propertiesAsMap)
                .build();
    }

    private InstructionSkeleton synonymInstruction(final String synonym) {
        return InstructionSkeleton.builder()
                .type(InstructionType.SYNONYM)
                .value(synonym)
                .build();
    }

    private InstructionSkeleton upInstruction(final String boostedTerm, final String parameter) {
        return InstructionSkeleton.builder()
                .type(InstructionType.UP)
                .parameter(parameter)
                .value(boostedTerm)
                .build();
    }

    private Property property(final String key, final Object value) {
        return Property.of(key, value);
    }

    private String asText(final RuleSkeleton... ruleSkeletons) {
        return MultiLineParser.toTextDefinition(Arrays.asList(ruleSkeletons));
    }

    private List<RuleSkeleton> parse(final String rulesAsText) {
        final MultiLineParser multiLineParser = parser();
        final StringReader stringReader = new StringReader(rulesAsText);

        try (final BufferedReader bufferedReader = new BufferedReader(stringReader)) {
            bufferedReader.lines().map(String::trim).forEach(multiLineParser::parse);

        } catch (IOException ignored) {}

        return multiLineParser.finish();
    }

    private MultiLineParser parser() {
        return MultiLineParser.builder()
                .inputSkeletonParser(InputSkeletonParser.create())
                .instructionSkeletonParser(InstructionSkeletonParser.create())
                .propertySkeletonParser(PropertySkeletonParser.create())
                .build();

    }

    @RequiredArgsConstructor(staticName = "of")
    @Getter
    private static class Property {
        private final String key;
        private final Object value;
    }
}
