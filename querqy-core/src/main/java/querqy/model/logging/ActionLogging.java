package querqy.model.logging;

import java.util.List;

public class ActionLogging {

    private final String message;
    private final MatchLogging match;
    private final List<InstructionLogging> instructions;

    public ActionLogging(final String message, final MatchLogging match, final List<InstructionLogging> instructions) {
        this.message = message;
        this.match = match;
        this.instructions = instructions;
    }

    public String getMessage() {
        return message;
    }

    public MatchLogging getMatch() {
        return match;
    }

    public List<InstructionLogging> getInstructions() {
        return instructions;
    }
}
