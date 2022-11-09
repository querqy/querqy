package querqy.rewrite;

import querqy.model.ExpandedQuery;
import querqy.rewrite.logging.RewriteChainLog;

import java.util.Optional;

public class RewriteChainOutput {

    private final ExpandedQuery expandedQuery;
    private final RewriteChainLog rewriteLog;

    private RewriteChainOutput(final ExpandedQuery expandedQuery, final RewriteChainLog rewriteLog) {
        this.expandedQuery = expandedQuery;
        this.rewriteLog = rewriteLog;
    }

    public ExpandedQuery getExpandedQuery() {
        return expandedQuery;
    }

    // TODO: needed to be optional?
    public Optional<RewriteChainLog> getRewriteLog() {
        return Optional.ofNullable(rewriteLog);
    }

    public static RewriteChainOutputBuilder builder() {
        return new RewriteChainOutputBuilder();
    }

    public static class RewriteChainOutputBuilder {

        private ExpandedQuery expandedQuery;
        private RewriteChainLog rewriteLog;

        public RewriteChainOutputBuilder expandedQuery(final ExpandedQuery expandedQuery) {
            this.expandedQuery = expandedQuery;
            return this;
        }

        public RewriteChainOutputBuilder rewriteLog(final RewriteChainLog rewriteLog) {
            this.rewriteLog = rewriteLog;
            return this;
        }

        public RewriteChainOutput build() {
            return new RewriteChainOutput(expandedQuery, rewriteLog);
        }
    }
}
