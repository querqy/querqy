package querqy.rewrite.rules.factory.config;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import querqy.rewrite.commonrules.QuerqyParserFactory;
import querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory;
import querqy.rewrite.rules.instruction.InstructionType;

import java.util.Set;

@Builder
@Getter
public class RuleParserConfig {

    @Default private final Set<InstructionType> allowedInstructionTypes = InstructionType.getAll();
    @Default private final QuerqyParserFactory querqyParserFactory = new WhiteSpaceQuerqyParserFactory();
    @Default private final boolean isAllowedToParseBooleanInput = false;
    @Default private final boolean generateMultiplicativeBoosts = false;

    public static RuleParserConfig defaultConfig() {
        return RuleParserConfig.builder().build();
    }

}
