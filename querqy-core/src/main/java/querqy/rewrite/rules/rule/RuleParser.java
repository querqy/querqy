package querqy.rewrite.rules.rule;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import querqy.model.Input;
import querqy.rewrite.commonrules.model.Instruction;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.InstructionsProperties;
import querqy.rewrite.commonrules.model.InstructionsSupplier;
import querqy.rewrite.rules.RuleParseException;
import querqy.rewrite.rules.input.InputAdapter;
import querqy.rewrite.rules.input.InputParserAdapter;
import querqy.rewrite.rules.instruction.InstructionParser;
import querqy.rewrite.rules.instruction.InstructionType;
import querqy.rewrite.rules.instruction.skeleton.InstructionSkeleton;
import querqy.rewrite.rules.property.PropertyParser;
import querqy.rewrite.rules.rule.skeleton.RuleSkeleton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static querqy.rewrite.rules.property.PropertyParser.ID;

@RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
public class RuleParser {

    private final List<Rule> rules = new ArrayList<>();
    private final Set<Object> knownIds = new HashSet<>();

    private final InputParserAdapter inputParserPrototype;
    private final InstructionParser instructionParserPrototype;
    private final PropertyParser propertyParser;

    private RuleSkeleton ruleSkeleton;
    private Integer ruleOrderNumber;

    @Builder
    private static RuleParser create(final InputParserAdapter inputParser,
                                     final InstructionParser instructionParser,
                                     final PropertyParser propertyParser
    ) {
        return RuleParser.of(inputParser, instructionParser, propertyParser);
    }

    public void parse(final RuleSkeleton ruleSkeleton, final int ruleOrderNumber) {
        this.ruleSkeleton = ruleSkeleton;
        this.ruleOrderNumber = ruleOrderNumber;

        final InputAdapter input = parseInput();
        final InstructionsProperties instructionsProperties = parseInstructionsProperties();
        final Instructions instructions = parseInstructions(input, instructionsProperties);

        if (input.isBooleanInput()) {
            evaluateBooleanInput(input);
            createBooleanInputLiterals(input, instructions);

        } else {
            rules.add(Rule.of((Input.SimpleInput) input.getInput(), new InstructionsSupplier(instructions)));
        }
    }

    private InputAdapter parseInput() {
        return inputParserPrototype
                .with(ruleSkeleton.getInputSkeleton())
                .parse();
    }

    private InstructionsProperties parseInstructionsProperties() {
        return propertyParser.parse(ruleSkeleton.getProperties(), createDefaultId());
    }

    private String createDefaultId() {
        return ruleSkeleton.getInputSkeleton() + "#" + ruleOrderNumber;
    }

    private Instructions parseInstructions(final InputAdapter input,
                                           final InstructionsProperties instructionsProperties) {

        final Object id = getId(instructionsProperties);
        validateId(id);

        final List<Instruction> instructions = instructionParserPrototype
                .with(input.getInputTerms(), ruleSkeleton.getInstructionSkeletons())
                .parse();

        return new Instructions(ruleOrderNumber, id, instructions, instructionsProperties);
    }

    private Object getId(final InstructionsProperties instructionsProperties) {
        return instructionsProperties.getProperty(ID)
                .orElseThrow(() -> new RuleParseException("Instructions have no ID"));
    }

    private void validateId(final Object id) {
        if (knownIds.contains(id)) {
            throw new RuleParseException("Duplicate ID: " + id);
        }

        knownIds.add(id);
    }

    private void evaluateBooleanInput(final InputAdapter input) {
        final List<InstructionType> instructionTypes = ruleSkeleton.getInstructionSkeletons()
                .stream()
                .map(InstructionSkeleton::getType)
                .collect(Collectors.toList());

        input.evaluateThatInstructionTypesAreSupported(instructionTypes);
    }

    private void createBooleanInputLiterals(final InputAdapter input, final Instructions instructions) {
        input.createBooleanInputLiterals(instructions);
    }

    public List<Rule> finish() {
        rules.addAll(inputParserPrototype.createRulesFromLiterals());
        return rules;
    }

}
