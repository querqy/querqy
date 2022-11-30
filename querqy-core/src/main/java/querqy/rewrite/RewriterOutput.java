package querqy.rewrite;

import querqy.model.ExpandedQuery;
import querqy.rewrite.logging.RewriterLog;

import java.util.Optional;

public class RewriterOutput {

    private final ExpandedQuery expandedQuery;
    private final RewriterLog rewriterLog;

    private RewriterOutput(final ExpandedQuery expandedQuery, final RewriterLog rewriterLog) {
        this.expandedQuery = expandedQuery;
        this.rewriterLog = rewriterLog;
    }

    public ExpandedQuery getExpandedQuery() {
        return expandedQuery;
    }

    public Optional<RewriterLog> getRewriterLog() {
        return Optional.ofNullable(rewriterLog);
    }

    public static RewriterOutputBuilder builder() {
        return new RewriterOutputBuilder();
    }

    public static class RewriterOutputBuilder {

        private ExpandedQuery expandedQuery;
        private RewriterLog rewriterLog;

        public RewriterOutputBuilder expandedQuery(final ExpandedQuery expandedQuery) {
            this.expandedQuery = expandedQuery;
            return this;
        }

        public RewriterOutputBuilder rewriterLog(final RewriterLog rewriterLog) {
            this.rewriterLog = rewriterLog;
            return this;
        }

        public RewriterOutput build() {
            return new RewriterOutput(expandedQuery, rewriterLog);
        }
    }
}
