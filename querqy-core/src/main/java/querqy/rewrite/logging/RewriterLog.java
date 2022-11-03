package querqy.rewrite.logging;

import java.util.LinkedList;
import java.util.List;

public class RewriterLog {

    private final boolean hasAppliedRewriting;
    private final List<ActionLog> actionLoggings;

    private RewriterLog(final boolean hasAppliedRewriting, final List<ActionLog> actionLoggings) {
        this.hasAppliedRewriting = hasAppliedRewriting;
        this.actionLoggings = actionLoggings;
    }

    public boolean hasAppliedRewriting() {
        return hasAppliedRewriting;
    }

    public List<ActionLog> getActionLoggings() {
        return actionLoggings;
    }

    public static RewriterLogBuilder builder() {
        return new RewriterLogBuilder();
    }

    public static class RewriterLogBuilder {

        private boolean hasAppliedRewriting;
        private List<ActionLog> actionLoggings;

        public RewriterLogBuilder hasAppliedRewriting(final boolean hasAppliedRewriting) {
            this.hasAppliedRewriting = hasAppliedRewriting;
            return this;
        }

        public RewriterLogBuilder addActionLogging(final ActionLog actionLogging) {
            if (this.actionLoggings == null) {
                this.actionLoggings = new LinkedList<>();
            }

            this.actionLoggings.add(actionLogging);
            return this;
        }

        public RewriterLogBuilder actionLoggings(final List<ActionLog> actionLoggings) {
            this.actionLoggings = actionLoggings;
            return this;
        }

        public RewriterLog build() {
            if (actionLoggings == null) {
                actionLoggings = List.of();
            }

            return new RewriterLog(hasAppliedRewriting, actionLoggings);
        }
    }
}
