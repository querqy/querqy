package querqy.model.rewriting;

import querqy.model.ExpandedQuery;
import querqy.model.logging.RewriteChainLogging;

import java.util.Optional;

public class RewriteChainOutput {

    private final ExpandedQuery expandedQuery;
    private final RewriteChainLogging rewriteLogging;

    private RewriteChainOutput(ExpandedQuery expandedQuery, RewriteChainLogging rewriteLogging) {
        this.expandedQuery = expandedQuery;
        this.rewriteLogging = rewriteLogging;
    }

    public ExpandedQuery getExpandedQuery() {
        return expandedQuery;
    }

    public Optional<RewriteChainLogging> getRewriteLogging() {
        return Optional.ofNullable(rewriteLogging);
    }

    public static RewriteChainOutputBuilder builder() {
        return new RewriteChainOutputBuilder();
    }

    public static class RewriteChainOutputBuilder {

        private ExpandedQuery expandedQuery;
        private RewriteChainLogging rewriteLogging;

        public RewriteChainOutputBuilder expandedQuery(final ExpandedQuery expandedQuery) {
            this.expandedQuery = expandedQuery;
            return this;
        }

        public RewriteChainOutputBuilder rewriteLogging(final RewriteChainLogging rewriteLogging) {
            this.rewriteLogging = rewriteLogging;
            return this;
        }

        public RewriteChainOutput build() {
            return new RewriteChainOutput(expandedQuery, rewriteLogging);
        }
    }
}
