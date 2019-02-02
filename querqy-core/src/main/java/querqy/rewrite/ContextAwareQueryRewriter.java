/**
 * 
 */
package querqy.rewrite;

import java.util.Map;

import querqy.model.ExpandedQuery;

/**
 * If a {@link QueryRewriter} implements this interface, the {@link #rewrite(ExpandedQuery, Map)} method is called 
 * instead of {@link #rewrite(ExpandedQuery)} in the query rewriting chain. The Map argument can be freely used to 
 * pass data between rewriters and to the consumer of the rewrite chain.
 * 
 * @author René Kriegler, @renekrie
 *
 */
public interface ContextAwareQueryRewriter extends QueryRewriter {

    /**
     * Name of the context key that acts as a flag to request debug information.
     */
    String CONTEXT_KEY_DEBUG_ENABLED = "querqy.debug.rewrite.isdebug";

    /**
     * The key under which debug information should be collected if CONTEXT_KEY_DEBUG_ENABLED is set.
     */
    String CONTEXT_KEY_DEBUG_DATA = "querqy.debug.rewrite.data";

    /**
     * @deprecated Use {@link #rewrite(ExpandedQuery, SearchEngineRequestAdapter)} instead.
     */
    @Deprecated
    ExpandedQuery rewrite(ExpandedQuery query, Map<String, Object> context);

    /**
     * Rewrite the query. The caller of this method should expect that the query that was passed as an argument to this
     * method could be modified.
     *
     * @param query The query to be rewritten
     * @param searchEngineRequestAdapter Encapsulates the request context.
     * @return The rewritten query.
     *
     */
    default ExpandedQuery rewrite(ExpandedQuery query, SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return rewrite(query, searchEngineRequestAdapter.getContext());
    }

}
