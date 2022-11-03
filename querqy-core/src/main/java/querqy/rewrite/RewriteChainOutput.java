package querqy.rewrite;

import querqy.model.ExpandedQuery;
import querqy.rewrite.logging.RewriteChainLog;

import java.util.Optional;

public class RewriteChainOutput {

    private final ExpandedQuery expandedQuery;
    private final RewriteChainLog rewriteLogging;

    private RewriteChainOutput(ExpandedQuery expandedQuery, RewriteChainLog rewriteLogging) {
        this.expandedQuery = expandedQuery;
        this.rewriteLogging = rewriteLogging;
    }

    public ExpandedQuery getExpandedQuery() {
        return expandedQuery;
    }

    // TODO: needed to be optional?
    public Optional<RewriteChainLog> getRewriteLogging() {
        return Optional.ofNullable(rewriteLogging);
    }

    public static RewriteChainOutputBuilder builder() {
        return new RewriteChainOutputBuilder();
    }

    public static class RewriteChainOutputBuilder {

        private ExpandedQuery expandedQuery;
        private RewriteChainLog rewriteLogging;

        public RewriteChainOutputBuilder expandedQuery(final ExpandedQuery expandedQuery) {
            this.expandedQuery = expandedQuery;
            return this;
        }

        public RewriteChainOutputBuilder rewriteLogging(final RewriteChainLog rewriteLogging) {
            this.rewriteLogging = rewriteLogging;
            return this;
        }

        public RewriteChainOutput build() {
            return new RewriteChainOutput(expandedQuery, rewriteLogging);
        }
    }
}
