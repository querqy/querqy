package querqy.rewrite;

import querqy.model.ExpandedQuery;
import querqy.rewrite.logging.RewriterLog;

import java.util.Optional;

public class RewriterOutput {

    private final ExpandedQuery expandedQuery;
    private final RewriterLog rewriterLogging;

    private RewriterOutput(final ExpandedQuery expandedQuery, final RewriterLog rewriterLogging) {
        this.expandedQuery = expandedQuery;
        this.rewriterLogging = rewriterLogging;
    }

    public ExpandedQuery getExpandedQuery() {
        return expandedQuery;
    }

    public Optional<RewriterLog> getRewriterLogging() {
        return Optional.ofNullable(rewriterLogging);
    }

    public static RewriterOutputBuilder builder() {
        return new RewriterOutputBuilder();
    }

    public static class RewriterOutputBuilder {

        private ExpandedQuery expandedQuery;
        private RewriterLog rewriterLogging;

        public RewriterOutputBuilder expandedQuery(final ExpandedQuery expandedQuery) {
            this.expandedQuery = expandedQuery;
            return this;
        }

        public RewriterOutputBuilder rewriterLogging(final RewriterLog rewriterLogging) {
            this.rewriterLogging = rewriterLogging;
            return this;
        }

        public RewriterOutput build() {
            return new RewriterOutput(expandedQuery, rewriterLogging);
        }
    }
}
