package querqy.explain;

import querqy.model.ExpandedQuery;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;

import java.util.Map;

public class SnapshotRewriterFactory extends RewriterFactory {

    final static String NAME = "__EXPLAIN_SNAPSHOT";

    private SnapshotRewriter rewriter = null;

    private final String previousRewriterId;

    public SnapshotRewriterFactory(final String previousRewriterId) {
        super(NAME + "/" + previousRewriterId);
        this.previousRewriterId = previousRewriterId;
    }

    @Override
    public synchronized QueryRewriter createRewriter(final ExpandedQuery input,
                                                     final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        if (rewriter != null) {
            throw new IllegalStateException("This factory can only be used once!");
        }
        this.rewriter = new SnapshotRewriter();
        return rewriter;
    }

    public Map<String, Object> getSnapshot() {
        return rewriter.getSnapshot();
    }

    public String getPreviousRewriterId() {
        return previousRewriterId;
    }
}
