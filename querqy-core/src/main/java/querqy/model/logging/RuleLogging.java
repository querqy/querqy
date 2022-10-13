package querqy.model.logging;

import java.util.List;

public class RuleLogging {

    private final String input;
    private final List<InstructionLogging> instructions;

    public RuleLogging(final String input, final List<InstructionLogging> instructions) {
        this.input = input;
        this.instructions = instructions;
    }

    public String getInput() {
        return input;
    }

    public List<InstructionLogging> getInstructions() {
        return instructions;
    }
}
