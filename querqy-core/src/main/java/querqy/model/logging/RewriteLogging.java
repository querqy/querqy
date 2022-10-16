package querqy.model.logging;

import java.util.LinkedList;
import java.util.List;

public class RewriteLogging {

    private final List<RewriteLoggingEntry> rewriteChain;

    private RewriteLogging(final List<RewriteLoggingEntry> rewriteChain) {
        this.rewriteChain = rewriteChain;
    }

    public List<RewriteLoggingEntry> getRewriteChain() {
        return rewriteChain;
    }

    public static class RewriteLoggingEntry {

        private final String rewriterId;
        private final List<ActionLogging> actions;

        private RewriteLoggingEntry(final String rewriterId, final List<ActionLogging> actions) {
            this.rewriterId = rewriterId;
            this.actions = actions;
        }

        public String getRewriterId() {
            return rewriterId;
        }

        public List<ActionLogging> getActions() {
            return actions;
        }
    }

    public static RewriteLoggingBuilder builder() {
        return new RewriteLoggingBuilder();
    }

    public static class RewriteLoggingBuilder {

        private final List<RewriteLoggingEntry> rewriteChain = new LinkedList<>();

        public RewriteLoggingBuilder add(final String rewriterId, final List<ActionLogging> actions) {
            rewriteChain.add(new RewriteLoggingEntry(rewriterId, actions));
            return this;
        }

        public RewriteLogging build() {
            return new RewriteLogging(rewriteChain);
        }
    }

}
