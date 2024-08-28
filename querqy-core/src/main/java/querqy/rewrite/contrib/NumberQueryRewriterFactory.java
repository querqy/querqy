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
    protected int minimumLengthOfResultingQueryTerm;

    public NumberQueryRewriterFactory(final String rewriterId) {

        this(rewriterId, false);
    }

    public NumberQueryRewriterFactory(final String rewriterId, final boolean acceptGeneratedTerms) {
        this(rewriterId, acceptGeneratedTerms, 3);
    }

    public NumberQueryRewriterFactory(final String rewriterId, final boolean acceptGeneratedTerms, final int minimumLengthOfResultingQueryTerm) {
        super(rewriterId);
        this.acceptGeneratedTerms = acceptGeneratedTerms;
        this.minimumLengthOfResultingQueryTerm = minimumLengthOfResultingQueryTerm;
    }

    @Override
    public QueryRewriter createRewriter(final ExpandedQuery input, final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return new NumberQueryRewriter(acceptGeneratedTerms, minimumLengthOfResultingQueryTerm);
    }

    @Override
    public Set<Term> getCacheableGenerableTerms() {
        return QueryRewriter.EMPTY_GENERABLE_TERMS;
    }

    public boolean isAcceptGeneratedTerms() {
        return acceptGeneratedTerms;
    }
}
