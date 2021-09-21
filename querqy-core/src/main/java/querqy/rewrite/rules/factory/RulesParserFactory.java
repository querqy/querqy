package querqy.rewrite.rules.factory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import querqy.rewrite.rules.RulesParser;
import querqy.rewrite.rules.factory.config.RulesParserConfig;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RulesParserFactory {

    public static RulesParser textParser(final RulesParserConfig rulesParserConfig) {
        return RulesParser.builder()
                .ruleSkeletonParser(
                        TextParserFactory.of(rulesParserConfig.getTextParserConfig()).createRuleSkeletonParser())
                .ruleParser(
                        RuleParserFactory.of(rulesParserConfig.getRuleParserConfig()).createRuleParser())
                .rulesCollectionBuilder(
                        rulesParserConfig.getRulesCollectionBuilder())
                .build();
    }
}
