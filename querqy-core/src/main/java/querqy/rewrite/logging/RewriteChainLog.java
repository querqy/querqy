package querqy.rewrite.logging;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class RewriteChainLog {

    private final List<RewriteLogEntry> rewriteChain;

    private RewriteChainLog(final List<RewriteLogEntry> rewriteChain) {
        this.rewriteChain = rewriteChain;
    }

    public List<RewriteLogEntry> getRewriteChain() {
        return rewriteChain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RewriteChainLog that = (RewriteChainLog) o;
        return Objects.equals(rewriteChain, that.rewriteChain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rewriteChain);
    }

    @Override
    public String toString() {
        return "RewriteChainLog{" +
                "rewriteChain=" + rewriteChain +
                '}';
    }

    public static class RewriteLogEntry {

        private final String rewriterId;
        private final List<ActionLog> actions;

        private RewriteLogEntry(final String rewriterId, final List<ActionLog> actions) {
            this.rewriterId = rewriterId;
            this.actions = actions;
        }

        public String getRewriterId() {
            return rewriterId;
        }

        public List<ActionLog> getActions() {
            return actions;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RewriteLogEntry that = (RewriteLogEntry) o;
            return Objects.equals(rewriterId, that.rewriterId) && Objects.equals(actions, that.actions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(rewriterId, actions);
        }

        @Override
        public String toString() {
            return "RewriteLogEntry{" +
                    "rewriterId='" + rewriterId + '\'' +
                    ", actions=" + actions +
                    '}';
        }
    }

    public static RewriteChainLogBuilder builder() {
        return new RewriteChainLogBuilder();
    }

    public static class RewriteChainLogBuilder {

        private final List<RewriteLogEntry> rewriteChain = new LinkedList<>();

        public RewriteChainLogBuilder add(final String rewriterId) {
            return add(rewriterId, Collections.emptyList());
        }

        public RewriteChainLogBuilder add(final String rewriterId, final List<ActionLog> actions) {
            rewriteChain.add(new RewriteLogEntry(rewriterId, actions));
            return this;
        }

        public RewriteChainLog build() {
            return new RewriteChainLog(rewriteChain);
        }
    }

}
