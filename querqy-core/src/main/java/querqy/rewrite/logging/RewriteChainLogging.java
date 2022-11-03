package querqy.rewrite.logging;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class RewriteChainLogging {

    private final List<RewriteLoggingEntry> rewriteChain;

    private RewriteChainLogging(final List<RewriteLoggingEntry> rewriteChain) {
        this.rewriteChain = rewriteChain;
    }

    public List<RewriteLoggingEntry> getRewriteChain() {
        return rewriteChain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RewriteChainLogging that = (RewriteChainLogging) o;
        return Objects.equals(rewriteChain, that.rewriteChain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rewriteChain);
    }

    @Override
    public String toString() {
        return "RewriteChainLogging{" +
                "rewriteChain=" + rewriteChain +
                '}';
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RewriteLoggingEntry that = (RewriteLoggingEntry) o;
            return Objects.equals(rewriterId, that.rewriterId) && Objects.equals(actions, that.actions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(rewriterId, actions);
        }

        @Override
        public String toString() {
            return "RewriteLoggingEntry{" +
                    "rewriterId='" + rewriterId + '\'' +
                    ", actions=" + actions +
                    '}';
        }
    }

    public static RewriteChainLoggingBuilder builder() {
        return new RewriteChainLoggingBuilder();
    }

    public static class RewriteChainLoggingBuilder {

        private final List<RewriteLoggingEntry> rewriteChain = new LinkedList<>();

        public RewriteChainLoggingBuilder add(final String rewriterId) {
            return add(rewriterId, Collections.emptyList());
        }

        public RewriteChainLoggingBuilder add(final String rewriterId, final List<ActionLogging> actions) {
            rewriteChain.add(new RewriteLoggingEntry(rewriterId, actions));
            return this;
        }

        public RewriteChainLogging build() {
            return new RewriteChainLogging(rewriteChain);
        }
    }

}
