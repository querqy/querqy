package querqy.model.rewriting;

import querqy.model.ExpandedQuery;
import querqy.model.logging.ActionLogging;

import java.util.List;

public class RewriterOutput {

    private final ExpandedQuery expandedQuery;
    private final List<ActionLogging> actionLoggings;

    public RewriterOutput(final ExpandedQuery expandedQuery, final List<ActionLogging> actionLoggings) {
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
}
