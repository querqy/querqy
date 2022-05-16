package querqy.rewrite.rules;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import querqy.model.Input;
import querqy.model.InputSequenceElement;
import querqy.rewrite.commonrules.RuleParseException;
import querqy.rewrite.commonrules.SimpleCommonRulesParser;
import querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory;
import querqy.rewrite.commonrules.model.Instruction;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.InstructionsSupplier;
import querqy.rewrite.commonrules.model.PositionSequence;
import querqy.rewrite.commonrules.model.RulesCollection;
import querqy.rewrite.commonrules.model.RulesCollectionBuilder;
import querqy.rewrite.commonrules.select.TopRewritingActionCollector;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputLiteral;
import querqy.rewrite.rules.factory.config.RuleParserConfig;
import querqy.rewrite.rules.factory.RulesParserFactory;
import querqy.rewrite.rules.factory.config.RulesParserConfig;
import querqy.rewrite.rules.factory.config.TextParserConfig;
import querqy.rewrite.rules.instruction.InstructionType;
import querqy.rewrite.rules.rule.Rule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
@RequiredArgsConstructor
public class ParserMigrationTest {

    private static final String BASE_PATH = "rule-parser-migration/";

    private final String rulesPath;

    @Parameterized.Parameters
    public static Collection<Object[]> ruleConfigurations() {
        return Arrays.asList(new Object[][] {
                { "0-single-synonym.txt" },
                { "1-all-instructions.txt" },
                { "2-multiple-rules.txt" },
                { "3-including-params.txt" },
                { "4-including-boolean-input.txt" },
                { "5-including-special-instructions.txt" },
                { "6-including-input-features.txt" },
                { "7-including-single-line-properties.txt" },
                { "8-including-multi-line-properties.txt" }
        });
    }

    @Test
    public void testThat_outputIsComparable_forBooleanInputAndWhiteSpaceQuerqyParser() {
        final TextParserConfig.Builder textParserConfigBuilder = TextParserConfig.builder()
                .isMultiLineRulesConfig(true);

        final RuleParserConfig ruleParserConfig = RuleParserConfig.builder()
                .isAllowedToParseBooleanInput(true)
                .allowedInstructionTypes(InstructionType.getAll())
                .querqyParserFactory(new WhiteSpaceQuerqyParserFactory())
                .build();

        compareOldToNew(textParserConfigBuilder, ruleParserConfig);
    }

    @Test
    public void testThat_outputIsComparable_forNoBooleanInputAndWhiteSpaceQuerqyParser() {
        final TextParserConfig.Builder textParserConfigBuilder = TextParserConfig.builder()
                .isMultiLineRulesConfig(true);

        final RuleParserConfig ruleParserConfig = RuleParserConfig.builder()
                .isAllowedToParseBooleanInput(false)
                .allowedInstructionTypes(InstructionType.getAll())
                .querqyParserFactory(new WhiteSpaceQuerqyParserFactory())
                .build();

        compareOldToNew(textParserConfigBuilder, ruleParserConfig);
    }

    @Test
    public void testThat_outputIsComparable_comparingOldToOld() {
        final TextParserConfig.Builder textParserConfigBuilder = TextParserConfig.builder()
                .isMultiLineRulesConfig(true);

        final RuleParserConfig ruleParserConfig = RuleParserConfig.builder()
                .isAllowedToParseBooleanInput(false)
                .allowedInstructionTypes(InstructionType.getAll())
                .querqyParserFactory(new WhiteSpaceQuerqyParserFactory())
                .build();

        compareOldToOld(textParserConfigBuilder, ruleParserConfig);
    }

    private void compareOldToOld(final TextParserConfig.Builder textParserConfigBuilder,
                                 final RuleParserConfig ruleParserConfig) {

        final TestRulesCollection rulesCollection1 = useOldParser(
                textParserConfigBuilder.rulesContentReader(reader(rulesPath)).build(), ruleParserConfig);
        final TestRulesCollection rulesCollection2 = useOldParser(
                textParserConfigBuilder.rulesContentReader(reader(rulesPath)).build(), ruleParserConfig);

        assertThat(rulesCollection1).isEqualTo(rulesCollection2);
    }

    private void compareOldToNew(final TextParserConfig.Builder textParserConfigBuilder,
                                 final RuleParserConfig ruleParserConfig) {

        final TestRulesCollection rulesCollection1 = useNewParser(
                textParserConfigBuilder.rulesContentReader(reader(rulesPath)).build(), ruleParserConfig);
        final TestRulesCollection rulesCollection2 = useOldParser(
                textParserConfigBuilder.rulesContentReader(reader(rulesPath)).build(), ruleParserConfig);

        assertThat(rulesCollection1).isEqualTo(rulesCollection2);
    }

    private TestRulesCollection useOldParser(final TextParserConfig textParserConfig,
                                             final RuleParserConfig ruleParserConfig) {

        final SimpleCommonRulesParser parser = createOldParser(textParserConfig, ruleParserConfig);

        try {
            return (TestRulesCollection) parser.parse();

        } catch (IOException | RuleParseException e) {
            throw new RuntimeException(e);
        }
    }

    private TestRulesCollection useNewParser(final TextParserConfig textParserConfig,
                                             final RuleParserConfig ruleParserConfig
    ) {
        try {
            return (TestRulesCollection) createNewParser(textParserConfig, ruleParserConfig).parse();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SimpleCommonRulesParser createOldParser(final TextParserConfig config, final RuleParserConfig ruleParserConfig) {
        return new SimpleCommonRulesParser(
                config.getRulesContentReader(),
                ruleParserConfig.isAllowedToParseBooleanInput(),
                ruleParserConfig.getQuerqyParserFactory(),
                TestRulesCollectionBuilder.create(),
                false);
    }

    private RulesParser createNewParser(final TextParserConfig config, final RuleParserConfig ruleParserConfig) {
        final RulesParserConfig rulesParserConfig = RulesParserConfig.builder()
                .textParserConfig(config)
                .ruleParserConfig(ruleParserConfig)
                .rulesCollectionBuilder(TestRulesCollectionBuilder.create())
                .build();

        return RulesParserFactory.textParser(rulesParserConfig);
    }

    private Reader reader(final String filePath) {
        final InputStream is = getClass().getClassLoader().getResourceAsStream(BASE_PATH + filePath);
        return new BufferedReader(new InputStreamReader(is));
    }


    @NoArgsConstructor(staticName = "create")
    private static class TestRulesCollectionBuilder implements RulesCollectionBuilder {
        private final List<Rule> rules = new ArrayList<>();

        @Override
        public void addRule(Input.SimpleInput input, Instructions instructions) {
            rules.add(Rule.of(input, new InstructionsSupplier(instructions)));
        }

        @Override
        public void addRule(Input.SimpleInput input, BooleanInputLiteral literal) {
            rules.add(Rule.of(input, new InstructionsSupplier(literal)));
        }

        @Override
        public void addRule(Rule rule) {
            rules.add(rule);
        }

        @Override
        public RulesCollection build() {
            return TestRulesCollection.of(rules);
        }
    }

    @RequiredArgsConstructor(staticName = "of")
    @EqualsAndHashCode
    @ToString(includeFieldNames = false)
    public static class TestRulesCollection implements RulesCollection {
        public final List<Rule> rules;


        @Override
        public void collectRewriteActions(PositionSequence<InputSequenceElement> sequence, TopRewritingActionCollector collector) {
        }

        @Override
        public Set<Instruction> getInstructions() {
            return null;
        }
    }
}
