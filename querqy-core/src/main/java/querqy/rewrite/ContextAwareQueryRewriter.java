package querqy.rewrite;

import querqy.model.ExpandedQuery;

/**
 * If a {@link QueryRewriter} implements this interface, the {@link #rewrite(ExpandedQuery, SearchEngineRequestAdapter)}
 * method is called instead of {@link #rewrite(ExpandedQuery)} in the query rewriting chain.
 * The SearchEngineRequestAdapter argument is used to handle info logging and debug info.
 *
 * @author René Kriegler, @renekrie
 *
 */
@Deprecated
public interface ContextAwareQueryRewriter extends QueryRewriter {

    /**
     * Rewrite the query. The caller of this method should expect that the query that was passed as an argument to this
     * method could be modified.
     *
     * @param query The query to be rewritten
     * @param searchEngineRequestAdapter Encapsulates the request context.
     * @return The rewritten query.
     *
     */
    // ExpandedQuery rewrite(ExpandedQuery query, SearchEngineRequestAdapter searchEngineRequestAdapter);

}
