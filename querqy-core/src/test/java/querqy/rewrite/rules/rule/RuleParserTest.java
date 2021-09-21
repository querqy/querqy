package querqy.rewrite.rules.rule;

import org.junit.Test;
import querqy.rewrite.rules.RuleParseException;
import querqy.rewrite.rules.factory.config.RuleParserConfig;
import querqy.rewrite.rules.factory.RuleParserFactory;
import querqy.rewrite.rules.property.PropertyParser;
import querqy.rewrite.rules.rule.skeleton.RuleSkeleton;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static querqy.rewrite.rules.RuleParserTestUtils.input;
import static querqy.rewrite.rules.RuleParserTestUtils.rules;
import static querqy.rewrite.rules.RuleParserTestUtils.skeletons;
import static querqy.rewrite.rules.RuleParserTestUtils.synonym;
import static querqy.rewrite.rules.RuleParserTestUtils.synonymSkeleton;
import static querqy.rewrite.rules.RuleParserTestUtils.ruleBuilder;
import static querqy.rewrite.rules.RuleParserTestUtils.skeletonBuilder;

public class RuleParserTest {

    @Test
    public void testThat_exceptionIsThrown_forRepeatedId() {
        final List<RuleSkeleton> skeletons = skeletons(
                skeletonBuilder()
                        .inputSkeleton("input1")
                        .instructionSkeleton(synonymSkeleton("synonym1"))
                        .property(PropertyParser.ID, "id")
                        .build(),
                skeletonBuilder()
                        .inputSkeleton("input2")
                        .instructionSkeleton(synonymSkeleton("synonym2"))
                        .property(PropertyParser.ID, "id")
                        .build()
        );

        assertThrows(RuleParseException.class, () -> parse(skeletons));
    }

    @Test
    public void testThat_rulesAreParsedProperly_forMultipleSkeletons() {
        final List<RuleSkeleton> skeletons = skeletons(
                skeletonBuilder()
                        .inputSkeleton("input1")
                        .instructionSkeleton(synonymSkeleton("synonym1"))
                        .property("key", "val")
                        .build(),
                skeletonBuilder()
                        .inputSkeleton("input2")
                        .instructionSkeleton(synonymSkeleton("synonym2"))
                        .property("key", "val")
                        .build()
        );

        final List<Rule> expectedRules = rules(
                ruleBuilder()
                        .input(input("input1"))
                        .ruleOrderNumber(0)
                        .id("input1#0")
                        .instruction(synonym("synonym1"))
                        .property("key", "val")
                        .property(PropertyParser.ID, "input1#0")
                        .property(PropertyParser.LOG_MESSAGE, "input1#0")
                        .build(),
                ruleBuilder()
                        .input(input("input2"))
                        .ruleOrderNumber(1)
                        .id("input2#1")
                        .instruction(synonym("synonym2"))
                        .property("key", "val")
                        .property(PropertyParser.ID, "input2#1")
                        .property(PropertyParser.LOG_MESSAGE, "input2#1")
                        .build()
        );

        final List<Rule> actualRules = parse(skeletons);

        assertThat(expectedRules).isEqualTo(actualRules);
    }

    private List<Rule> parse(final List<RuleSkeleton> ruleSkeletons) {
        final RuleParser parser = parser();

        IntStream.range(0, ruleSkeletons.size())
                .forEach(index -> parser.parse(ruleSkeletons.get(index), index));

        return parser.finish();
    }

    private static RuleParser parser() {
        return RuleParserFactory.of(RuleParserConfig.defaultConfig()).createRuleParser();
    }
}
