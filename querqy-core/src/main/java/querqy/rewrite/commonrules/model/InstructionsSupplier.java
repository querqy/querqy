package querqy.rewrite.commonrules.model;

import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputLiteral;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class InstructionsSupplier {

    private final List<Instructions> instructionsList;
    private BooleanInputLiteral literal = null;

    public InstructionsSupplier() {
        this.instructionsList = new LinkedList<>();
    }

    public InstructionsSupplier addInstructions(final Instructions instructions) {
        this.instructionsList.add(instructions);
        return this;
    }

    public InstructionsSupplier setLiteral(final BooleanInputLiteral literal) {
        this.literal = literal;
        return this;
    }

    public void merge(final InstructionsSupplier instructionsSupplier) {
        this.instructionsList.addAll(instructionsSupplier.getInstructionsList());

        if (this.literal == null) {
            this.literal = instructionsSupplier.literal;
        }
    }

    public boolean hasInstructions() {
        return !this.instructionsList.isEmpty();
    }
    public List<Instructions> getInstructionsList() {
        return instructionsList;
    }

    public Optional<BooleanInputLiteral> getLiteral() {
        return Optional.ofNullable(literal);
    }
}
