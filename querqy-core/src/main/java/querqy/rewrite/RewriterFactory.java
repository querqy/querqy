/**
 * 
 */
package querqy.rewrite;

import java.util.Set;

import querqy.model.ExpandedQuery;
import querqy.model.Term;

/**
 * @author rene
 *
 */
public abstract class RewriterFactory {

    private final String rewriterId;

    protected RewriterFactory(final String rewriterId) {
        this.rewriterId = rewriterId;
    }

    public abstract QueryRewriter createRewriter(ExpandedQuery input,
                                                 SearchEngineRequestAdapter searchEngineRequestAdapter);

    public abstract Set<Term> getGenerableTerms();

    public String getRewriterId() {
        return rewriterId;
    }


}
