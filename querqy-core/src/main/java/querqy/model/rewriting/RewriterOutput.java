package querqy.model.rewriting;

import querqy.model.ExpandedQuery;
import querqy.model.logging.ActionLogging;

import java.util.List;

public class RewriterOutput {

    private final ExpandedQuery expandedQuery;
    private final List<ActionLogging> actionLoggings;

    private RewriterOutput(final ExpandedQuery expandedQuery, final List<ActionLogging> actionLoggings) {
        this.expandedQuery = expandedQuery;
        this.actionLoggings = actionLoggings;
    }

    public RewriterOutput(final ExpandedQuery expandedQuery) {
        this(expandedQuery, List.of());
    }

    public ExpandedQuery getExpandedQuery() {
        return expandedQuery;
    }

    public List<ActionLogging> getActionLoggings() {
        return actionLoggings;
    }

    public static RewriterOutputBuilder builder() {
        return new RewriterOutputBuilder();
    }

    public static class RewriterOutputBuilder {

        private ExpandedQuery expandedQuery;
        private List<ActionLogging> actionLoggings;

        public RewriterOutputBuilder expandedQuery(final ExpandedQuery expandedQuery) {
            this.expandedQuery = expandedQuery;
            return this;
        }

        public RewriterOutputBuilder actionLoggings(final List<ActionLogging> actionLoggings) {
            this.actionLoggings = actionLoggings;
            return this;
        }

        public RewriterOutput build() {
            return new RewriterOutput(expandedQuery, actionLoggings);
        }
    }
}
