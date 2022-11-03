package querqy.model.rewriting;

import querqy.model.ExpandedQuery;
import querqy.rewrite.logging.RewriterLogging;

import java.util.Optional;

public class RewriterOutput {

    private final ExpandedQuery expandedQuery;
    private final RewriterLogging rewriterLogging;

    private RewriterOutput(final ExpandedQuery expandedQuery, final RewriterLogging rewriterLogging) {
        this.expandedQuery = expandedQuery;
        this.rewriterLogging = rewriterLogging;
    }

    public ExpandedQuery getExpandedQuery() {
        return expandedQuery;
    }

    public Optional<RewriterLogging> getRewriterLogging() {
        return Optional.ofNullable(rewriterLogging);
    }

    public static RewriterOutputBuilder builder() {
        return new RewriterOutputBuilder();
    }

    public static class RewriterOutputBuilder {

        private ExpandedQuery expandedQuery;
        private RewriterLogging rewriterLogging;

        public RewriterOutputBuilder expandedQuery(final ExpandedQuery expandedQuery) {
            this.expandedQuery = expandedQuery;
            return this;
        }

        public RewriterOutputBuilder rewriterLogging(final RewriterLogging rewriterLogging) {
            this.rewriterLogging = rewriterLogging;
            return this;
        }

        public RewriterOutput build() {
            return new RewriterOutput(expandedQuery, rewriterLogging);
        }
    }
}
