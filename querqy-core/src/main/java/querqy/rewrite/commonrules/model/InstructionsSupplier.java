package querqy.rewrite.commonrules.model;

import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputLiteral;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class InstructionsSupplier {

    private final List<Instructions> instructionsList = new LinkedList<>();
    private BooleanInputLiteral literal = null;

    public InstructionsSupplier(final List<Instructions> instructionsList, final BooleanInputLiteral literal) {
        if (instructionsList != null) {
            this.instructionsList.addAll(instructionsList);
        }
        this.literal = literal;
    }

    public InstructionsSupplier(final Instructions instructions) {
        instructionsList.add(instructions);
    }

    public InstructionsSupplier(final BooleanInputLiteral literal) {
        this(Collections.emptyList(), literal);
    }

    public void merge(final InstructionsSupplier instructionsSupplier) {

        if (this.literal != null) {
            instructionsSupplier.getLiteral().ifPresent(otherLiteral -> {
                if (!literal.equals(otherLiteral)) {
                    throw new IllegalArgumentException(String.format("Literals not equal: %s != %s",
                            String.join(" ", literal.getTerms()),
                            String.join(" ", otherLiteral.getTerms())
                    ));
                }
            });
        } else {
            this.literal = instructionsSupplier.literal;
        }

        this.instructionsList.addAll(instructionsSupplier.getInstructionsList());
    }

    public boolean hasInstructions() {
        return !instructionsList.isEmpty();
    }
    public List<Instructions> getInstructionsList() {
        return instructionsList;
    }

    public Optional<BooleanInputLiteral> getLiteral() {
        return Optional.ofNullable(literal);
    }
}
