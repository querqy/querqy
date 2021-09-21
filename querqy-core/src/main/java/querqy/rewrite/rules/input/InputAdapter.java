package querqy.rewrite.rules.input;

import lombok.Builder;
import lombok.Getter;
import querqy.model.Input;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.Term;
import querqy.rewrite.commonrules.select.booleaninput.BooleanInputParser;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputElement;
import querqy.rewrite.rules.RuleParseException;
import querqy.rewrite.rules.instruction.InstructionType;

import java.util.Collections;
import java.util.List;

import static querqy.rewrite.rules.instruction.InstructionType.DELETE;
import static querqy.rewrite.rules.instruction.InstructionType.SYNONYM;

@Builder
public class InputAdapter {

    @Getter private final Input input;
    private final BooleanInputParser booleanInputParser;
    private final String inputSkeleton;

    public List<Term> getInputTerms() {
        return input != null ? input.getInputTerms() : Collections.emptyList();
    }

    public boolean isBooleanInput() {
        return input instanceof Input.BooleanInput;
    }

    public void evaluateThatInstructionTypesAreSupported(final List<InstructionType> instructionTypes) {
        if (isBooleanInput() && instructionTypes.stream()
                .anyMatch(instructionType -> instructionType == DELETE || instructionType == SYNONYM)){
            throw new RuleParseException("SYNONYM and DELETE instructions are not allowed for boolean input");
        }
    }

    public void createBooleanInputLiterals(final Instructions instructions) {
        if (booleanInputParser != null && isBooleanInput()) {
            final List<BooleanInputElement> elements = ((Input.BooleanInput) input).getElements();
            try {
                booleanInputParser.createInputBuilder(elements, inputSkeleton).withInstructions(instructions).build();

            } catch (querqy.rewrite.commonrules.RuleParseException e) {
                throw new RuleParseException(e.getMessage());
            }
        }
    }

}
