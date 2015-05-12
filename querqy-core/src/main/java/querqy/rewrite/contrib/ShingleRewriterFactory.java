package querqy.rewrite.contrib;

import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;

import java.util.Map;
import java.util.Set;

/**
 * Factory for {@link ShingleRewriter}
 */
public class ShingleRewriterFactory implements RewriterFactory {
    
    protected final boolean acceptGeneratedTerms;

    public ShingleRewriterFactory() {
        this(false);
    }

    public ShingleRewriterFactory(boolean acceptGeneratedTerms){
        this.acceptGeneratedTerms = acceptGeneratedTerms;
    }

    @Override
    public QueryRewriter createRewriter(ExpandedQuery input, Map<String, ?> context) {
        return new ShingleRewriter(acceptGeneratedTerms);
    }

    @Override
    public Set<Term> getGenerableTerms() {
        return QueryRewriter.EMPTY_GENERABLE_TERMS;
    }

}
