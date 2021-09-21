package querqy.rewrite.rules.factory;

import lombok.RequiredArgsConstructor;
import querqy.rewrite.rules.factory.config.TextParserConfig;
import querqy.rewrite.rules.input.skeleton.InputSkeletonParser;
import querqy.rewrite.rules.instruction.skeleton.InstructionSkeletonParser;
import querqy.rewrite.rules.property.skeleton.PropertySkeletonParser;
import querqy.rewrite.rules.rule.skeleton.LineParser;
import querqy.rewrite.rules.rule.skeleton.MultiLineParser;
import querqy.rewrite.rules.rule.skeleton.TextRuleSkeletonParser;
import querqy.rewrite.rules.rule.skeleton.SingleLineParser;

@RequiredArgsConstructor(staticName = "of")
public class TextParserFactory {

    private final TextParserConfig textParserConfig;

    public TextRuleSkeletonParser createRuleSkeletonParser() {
        final LineParser lineParser = createLineParser();

        return TextRuleSkeletonParser.builder()
                .rulesContentReader(textParserConfig.getRulesContentReader())
                .lineParser(lineParser)
                .lineNumberMappings(textParserConfig.getLineNumberMappings())
                .build();
    }

    private LineParser createLineParser() {
        return textParserConfig.isMultiLineRulesConfig()
                ? createMultiLineParser()
                : SingleLineParser.create();
    }

    private MultiLineParser createMultiLineParser() {
        return MultiLineParser.builder()
                .inputSkeletonParser(InputSkeletonParser.create())
                .instructionSkeletonParser(InstructionSkeletonParser.create())
                .propertySkeletonParser(PropertySkeletonParser.create())
                .build();
    }


}
