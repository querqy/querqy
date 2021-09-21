package querqy.rewrite.rules.rule.skeleton;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import querqy.rewrite.rules.RuleParseException;
import querqy.rewrite.rules.input.skeleton.InputSkeletonParser;
import querqy.rewrite.rules.instruction.skeleton.InstructionSkeletonParser;
import querqy.rewrite.rules.property.skeleton.PropertySkeletonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MultiLineParser implements LineParser {

    private final List<RuleSkeleton> ruleSkeletons = new ArrayList<>();

    private final InputSkeletonParser inputSkeletonParser;
    private final InstructionSkeletonParser instructionSkeletonParser;
    private final PropertySkeletonParser propertySkeletonParser;

    @Builder
    private MultiLineParser(final InputSkeletonParser inputSkeletonParser,
                            final InstructionSkeletonParser instructionSkeletonParser,
                            final PropertySkeletonParser propertySkeletonParser) {

        this.inputSkeletonParser = inputSkeletonParser;
        this.instructionSkeletonParser = instructionSkeletonParser;
        this.propertySkeletonParser = propertySkeletonParser;
    }

    private RuleSkeleton.RuleSkeletonBuilder ruleSkeletonBuilder;

    @Override
    public void parse(final String line) {
        setLine(line);

        if (isFirstLine()) {
            parseFirstLine();

        } else {
            parseLine();
        }
    }

    private void setLine(final String line) {
        inputSkeletonParser.setContent(line);
        instructionSkeletonParser.setContent(line);
        propertySkeletonParser.setContent(line);
    }

    private boolean isFirstLine() {
        return ruleSkeletonBuilder == null;
    }

    protected void parseFirstLine() {
        if (isInput()) {
            initiateRuleBuilding();
            parseAsInput();

        } else {
            throw new RuleParseException("Rule definitions must always begin with an input");
        }
    }

    private boolean isInput() {
        return inputSkeletonParser.isParsable();
    }

    protected void initiateRuleBuilding() {
        ruleSkeletonBuilder = RuleSkeleton.builder();
    }

    protected void parseAsInput() {
        inputSkeletonParser.parse();
        ruleSkeletonBuilder.inputSkeleton(inputSkeletonParser.finish());
    }

    protected void parseLine() {
        /*
         * The definition of multiline rules (e.g. common rules) allows defining property groups over multiple
         * lines. These lines can potentially match the pattern of input or instruction, even though they are part
         * of a multiline property definition. This is why the parser first must check whether the new line is a
         * property or part of a multiline property definition that has been initiated in lines before
         * and has not been closed, yet.
         */
        if (isProperty()) {
            parseAsProperty();

        } else if (isInput()) {
            finishRuleBuilding();
            initiateRuleBuilding();
            parseAsInput();

        } else if (isInstruction()) {
            parseAsInstruction();

        } else {
            throw new RuleParseException("Line cannot be parsed");
        }
    }

    protected boolean isProperty() {
        return propertySkeletonParser.isParsable();
    }

    protected void parseAsProperty() {
        propertySkeletonParser.parse();
    }

    protected void finishRuleBuilding() {
        if (propertySkeletonParser.hasProperties()) {
            ruleSkeletonBuilder.properties(propertySkeletonParser.finish());
        }

        final RuleSkeleton ruleSkeleton = ruleSkeletonBuilder.build();
        ruleSkeletons.add(ruleSkeleton);
    }

    private boolean isInstruction() {
        return instructionSkeletonParser.isParsable();
    }

    protected void parseAsInstruction() {
        instructionSkeletonParser.parse();
        ruleSkeletonBuilder.instructionSkeleton(instructionSkeletonParser.finish());
    }

    @Override
    public List<RuleSkeleton> finish() {
        if (hasInitiatedRuleBuilding()) {
            finishRuleBuilding();
        }

        return ruleSkeletons;
    }

    private boolean hasInitiatedRuleBuilding() {
        return ruleSkeletonBuilder != null;
    }

    public static String toTextDefinition(final List<RuleSkeleton> ruleSkeletons) {
        final List<String> parts = ruleSkeletons.stream()
                .map(MultiLineParser::toTextDefinition)
                .collect(Collectors.toList());

        return String.join("\n", parts);
    }

    public static String toTextDefinition(final RuleSkeleton ruleSkeleton) {
        final RuleDefinitionSerializer serializer = RuleDefinitionSerializer.of(ruleSkeleton);
        return serializer.toTextDefinition();
    }

    @RequiredArgsConstructor(staticName = "of")
    public static class RuleDefinitionSerializer {
        private final RuleSkeleton ruleSkeleton;
        private final List<String> parts = new ArrayList<>();

        public String toTextDefinition() {
            inputToText();
            instructionsToText();
            propertiesToText();

            return String.join("\n  ", parts);
        }

        private void inputToText() {
            parts.add(
                    InputSkeletonParser.toTextDefinition(ruleSkeleton.getInputSkeleton()));
        }

        private void instructionsToText() {
            ruleSkeleton.getInstructionSkeletons().stream()
                    .map(InstructionSkeletonParser::toTextDefinition)
                    .forEach(parts::add);
        }

        private void propertiesToText() {
            if (ruleSkeleton.hasProperties()) {
                final String propertiesAsText = PropertySkeletonParser.toTextDefinition(ruleSkeleton.getProperties());

                try (final BufferedReader bufferedReader = new BufferedReader(new StringReader(propertiesAsText))) {
                    bufferedReader.lines().forEach(parts::add);

                } catch (IOException ignored) {
                    // should not happen
                }
            }
        }
    }

}
