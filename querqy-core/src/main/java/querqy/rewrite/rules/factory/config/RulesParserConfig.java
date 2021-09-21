package querqy.rewrite.rules.factory.config;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import querqy.rewrite.commonrules.model.RulesCollectionBuilder;

@Builder
@Getter
public class RulesParserConfig {

    @Default private final TextParserConfig textParserConfig = TextParserConfig.defaultConfig();
    @Default private final RuleParserConfig ruleParserConfig = RuleParserConfig.defaultConfig();

    @NonNull private final RulesCollectionBuilder rulesCollectionBuilder;

}
