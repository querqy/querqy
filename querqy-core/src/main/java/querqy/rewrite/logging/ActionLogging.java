package querqy.rewrite.logging;

import java.util.List;

public class ActionLogging {

    private final String message;
    private final MatchLogging match;
    private final List<InstructionLogging> instructions;

    private ActionLogging(final String message, final MatchLogging match, final List<InstructionLogging> instructions) {
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

    public static ActionLoggingBuilder builder() {
        return new ActionLoggingBuilder();
    }

    public static class ActionLoggingBuilder {

        private String message;
        private MatchLogging match;
        private List<InstructionLogging> instructions;

        public ActionLoggingBuilder message(final String message) {
            this.message = message;
            return this;
        }

        public ActionLoggingBuilder match(final MatchLogging match) {
            this.match = match;
            return this;
        }

        public ActionLoggingBuilder instructions(final List<InstructionLogging> instructions) {
            this.instructions = instructions;
            return this;
        }

        public ActionLogging build() {
            return new ActionLogging(message, match, instructions);
        }
    }
}
