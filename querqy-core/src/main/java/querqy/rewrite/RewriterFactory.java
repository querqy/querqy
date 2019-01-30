/**
 * 
 */
package querqy.rewrite;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import querqy.model.ExpandedQuery;
import querqy.model.Term;

/**
 * @author rene
 *
 */
public interface RewriterFactory {

    /**
     * @deprecated Use {@link #createRewriter(ExpandedQuery, SearchEngineRequestAdapter)} instead.
     */
    @Deprecated
    QueryRewriter createRewriter(ExpandedQuery input, Map<String, ?> context);

    default QueryRewriter createRewriter(ExpandedQuery input, SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return createRewriter(input, searchEngineRequestAdapter.getContext());
    }

    Set<Term> getGenerableTerms();

}
