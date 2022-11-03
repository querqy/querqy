package querqy.rewrite.logging;

import java.util.List;

public class ActionLog {

    private final String message;
    private final MatchLog match;
    private final List<InstructionLog> instructions;

    private ActionLog(final String message, final MatchLog match, final List<InstructionLog> instructions) {
        this.message = message;
        this.match = match;
        this.instructions = instructions;
    }

    public String getMessage() {
        return message;
    }

    public MatchLog getMatch() {
        return match;
    }

    public List<InstructionLog> getInstructions() {
        return instructions;
    }

    public static ActionLogBuilder builder() {
        return new ActionLogBuilder();
    }

    public static class ActionLogBuilder {

        private String message;
        private MatchLog match;
        private List<InstructionLog> instructions;

        public ActionLogBuilder message(final String message) {
            this.message = message;
            return this;
        }

        public ActionLogBuilder match(final MatchLog match) {
            this.match = match;
            return this;
        }

        public ActionLogBuilder instructions(final List<InstructionLog> instructions) {
            this.instructions = instructions;
            return this;
        }

        public ActionLog build() {
            return new ActionLog(message, match, instructions);
        }
    }
}
