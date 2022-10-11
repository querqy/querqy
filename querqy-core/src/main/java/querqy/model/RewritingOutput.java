package querqy.model;

public class RewritingOutput {

    private final ExpandedQuery expandedQuery;

    public RewritingOutput(final ExpandedQuery expandedQuery) {
        this.expandedQuery = expandedQuery;
    }

    public ExpandedQuery getExpandedQuery() {
        return expandedQuery;
    }
}
