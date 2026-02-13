package querqy.rewrite.contrib.replace;

import querqy.model.AbstractNodeVisitor;
import querqy.model.ExpandedQuery;
import querqy.model.Node;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterOutput;
import querqy.rewrite.SearchEngineRequestAdapter;

public class RegexReplaceRewriter extends AbstractNodeVisitor<Node> implements QueryRewriter {
    @Override
    public RewriterOutput rewrite(final ExpandedQuery query,
                                  final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return null;
    }
}
