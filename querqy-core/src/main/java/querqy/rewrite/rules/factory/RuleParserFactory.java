package querqy.rewrite.rules.factory;

import lombok.RequiredArgsConstructor;
import querqy.rewrite.rules.factory.config.RuleParserConfig;
import querqy.rewrite.rules.input.InputParserAdapter;
import querqy.rewrite.rules.instruction.InstructionParser;
import querqy.rewrite.rules.property.PropertyParser;
import querqy.rewrite.rules.query.QuerqyQueryParser;
import querqy.rewrite.rules.query.TermsParser;
import querqy.rewrite.rules.rule.RuleParser;

@RequiredArgsConstructor(staticName = "of")
public class RuleParserFactory {

    private final RuleParserConfig ruleParserConfig;

    public RuleParser createRuleParser() {
        return RuleParser.builder()
                .inputParser(createInputParserAdapter())
                .instructionParser(createInstructionParser())
                .propertyParser(PropertyParser.create())
                .build();
    }

    private InputParserAdapter createInputParserAdapter() {
        return InputParserAdapter.builder()
                .isAllowedToParseBooleanInput(ruleParserConfig.isAllowedToParseBooleanInput())
                .build();
    }

    private InstructionParser createInstructionParser() {
        return InstructionParser.prototypeBuilder()
                .querqyQueryParser(
                        QuerqyQueryParser.createPrototypeOf(ruleParserConfig.getQuerqyParserFactory()))
                .termsParser(TermsParser.createPrototype())
                .supportedTypes(ruleParserConfig.getAllowedInstructionTypes())
                .boostMethod(ruleParserConfig.getBoostMethod())
                .build();
    }

}
