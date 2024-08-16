package querqy.rewrite.contrib;

import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;

import java.util.Set;

/**
 * Factory for {@link NumberQueryRewriter}
 */
public class NumberQueryRewriterFactory extends RewriterFactory {

    protected final boolean acceptGeneratedTerms;

    public NumberQueryRewriterFactory(final String rewriterId) {

        this(rewriterId, false);
    }

    public NumberQueryRewriterFactory(final String rewriterId, final boolean acceptGeneratedTerms) {
        super(rewriterId);
        this.acceptGeneratedTerms = acceptGeneratedTerms;
    }

    @Override
    public QueryRewriter createRewriter(ExpandedQuery input, SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return new NumberQueryRewriter(acceptGeneratedTerms);
    }

    @Override
    public Set<Term> getCacheableGenerableTerms() {
        return QueryRewriter.EMPTY_GENERABLE_TERMS;
    }

    public boolean isAcceptGeneratedTerms() {
        return acceptGeneratedTerms;
    }
}
