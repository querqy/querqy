package querqy.rewrite.contrib;

import querqy.model.ExpandedQuery;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;

import java.util.Map;

/**
 * Factory for {@link ShingleRewriter}
 */
public class ShingleRewriterFactory implements RewriterFactory {
    private boolean acceptGeneratedTerms;

    public ShingleRewriterFactory(boolean acceptGeneratedTerms){
        this.acceptGeneratedTerms = acceptGeneratedTerms;
    }

    @Override
    public QueryRewriter createRewriter(ExpandedQuery input, Map<String, ?> context) {
        return new ShingleRewriter(acceptGeneratedTerms);
    }

}
