package querqy.rewrite.contrib;

import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;

import java.util.Map;
import java.util.Set;

/**
 * Factory for {@link ShingleRewriter}
 */
public class ShingleRewriterFactory extends RewriterFactory {

    protected final boolean acceptGeneratedTerms;

    public ShingleRewriterFactory(final String rewriterId) {

        this(rewriterId, false);
    }

    public ShingleRewriterFactory(final String rewriterId, final boolean acceptGeneratedTerms) {
        super(rewriterId);
        this.acceptGeneratedTerms = acceptGeneratedTerms;
    }

    @Override
    public QueryRewriter createRewriter(ExpandedQuery input, SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return new ShingleRewriter(acceptGeneratedTerms);
    }

    @Override
    public Set<Term> getCacheableGenerableTerms() {
        return QueryRewriter.EMPTY_GENERABLE_TERMS;
    }

    public boolean isAcceptGeneratedTerms() {
        return acceptGeneratedTerms;
    }
}
