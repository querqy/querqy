package querqy.rewrite.logging;

import java.util.LinkedList;
import java.util.List;

public class RewriterLogging {

    private final boolean hasAppliedRewriting;
    private final List<ActionLogging> actionLoggings;

    private RewriterLogging(final boolean hasAppliedRewriting, final List<ActionLogging> actionLoggings) {
        this.hasAppliedRewriting = hasAppliedRewriting;
        this.actionLoggings = actionLoggings;
    }

    public boolean hasAppliedRewriting() {
        return hasAppliedRewriting;
    }

    public List<ActionLogging> getActionLoggings() {
        return actionLoggings;
    }

    public static RewriterLoggingBuilder builder() {
        return new RewriterLoggingBuilder();
    }

    public static class RewriterLoggingBuilder {

        private boolean hasAppliedRewriting;
        private List<ActionLogging> actionLoggings;

        public RewriterLoggingBuilder hasAppliedRewriting(final boolean hasAppliedRewriting) {
            this.hasAppliedRewriting = hasAppliedRewriting;
            return this;
        }

        public RewriterLoggingBuilder addActionLogging(final ActionLogging actionLogging) {
            if (this.actionLoggings == null) {
                this.actionLoggings = new LinkedList<>();
            }

            this.actionLoggings.add(actionLogging);
            return this;
        }

        public RewriterLoggingBuilder actionLoggings(final List<ActionLogging> actionLoggings) {
            this.actionLoggings = actionLoggings;
            return this;
        }

        public RewriterLogging build() {
            if (actionLoggings == null) {
                actionLoggings = List.of();
            }

            return new RewriterLogging(hasAppliedRewriting, actionLoggings);
        }
    }
}
