package querqy.rewrite.logging;

import java.util.LinkedList;
import java.util.List;

public class RewriterLog {

    private final boolean hasAppliedRewriting;
    private final List<ActionLog> actionLogs;

    private RewriterLog(final boolean hasAppliedRewriting, final List<ActionLog> actionLog) {
        this.hasAppliedRewriting = hasAppliedRewriting;
        this.actionLogs = actionLog;
    }

    public boolean hasAppliedRewriting() {
        return hasAppliedRewriting;
    }

    public List<ActionLog> getActionLogs() {
        return actionLogs;
    }

    public static RewriterLogBuilder builder() {
        return new RewriterLogBuilder();
    }

    public static class RewriterLogBuilder {

        private boolean hasAppliedRewriting;
        private List<ActionLog> actionLogs;

        public RewriterLogBuilder hasAppliedRewriting(final boolean hasAppliedRewriting) {
            this.hasAppliedRewriting = hasAppliedRewriting;
            return this;
        }

        public RewriterLogBuilder addActionLogs(final ActionLog actionLogs) {
            if (this.actionLogs == null) {
                this.actionLogs = new LinkedList<>();
            }

            this.actionLogs.add(actionLogs);
            return this;
        }

        public RewriterLogBuilder actionLogs(final List<ActionLog> actionLogs) {
            this.actionLogs = actionLogs;
            return this;
        }

        public RewriterLog build() {
            if (actionLogs == null) {
                actionLogs = List.of();
            }

            return new RewriterLog(hasAppliedRewriting, actionLogs);
        }
    }
}
