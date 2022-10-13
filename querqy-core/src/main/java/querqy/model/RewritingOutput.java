package querqy.model;

import querqy.model.logging.ActionLogging;

import java.util.List;

public class RewritingOutput {

    private final ExpandedQuery expandedQuery;
    private final List<ActionLogging> actionLoggings;

    public RewritingOutput(final ExpandedQuery expandedQuery, final List<ActionLogging> actionLoggings) {
        this.expandedQuery = expandedQuery;
        this.actionLoggings = actionLoggings;
    }

    public RewritingOutput(final ExpandedQuery expandedQuery) {
        this(expandedQuery, List.of());
    }

    public ExpandedQuery getExpandedQuery() {
        return expandedQuery;
    }

    public List<ActionLogging> getActionLoggings() {
        return actionLoggings;
    }
}
