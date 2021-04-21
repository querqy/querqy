/**
 *
 */
package querqy.rewrite;

import querqy.infologging.InfoLoggingContext;
import querqy.model.AbstractNodeVisitor;
import querqy.model.ExpandedQuery;
import querqy.model.Node;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * If a {@link QueryRewriter} implements this interface, the {@link #rewrite(ExpandedQuery, SearchEngineRequestAdapter)}
 * method is called instead of {@link #rewrite(ExpandedQuery)} in the query rewriting chain. The Map argument can be
 * used to pass data between rewriters and to the consumer of the rewrite chain. It also gives you the easy possibility to
 * track the applied rules in a rewriter.
 *
 * @author René Kriegler, @renekrie
 *
 */
public abstract class ContextAwareQueryRewriter extends AbstractNodeVisitor<Node> implements QueryRewriter {

    /**
     * Name of the context key that acts as a flag to request debug information.
     */
    public static final String CONTEXT_KEY_DEBUG_ENABLED = "querqy.debug.rewrite.isdebug";

    /**
     * The key under which debug information should be collected if CONTEXT_KEY_DEBUG_ENABLED is set.
     */
    public static final String CONTEXT_KEY_DEBUG_DATA = "querqy.debug.rewrite.data";

    /**
     * Marker in the infologging for all applied rules.
     */
    private static final String APPLIED_RULES = "APPLIED_RULES";

    protected SearchEngineRequestAdapter searchEngineRequestAdapter;

    /**
     * This is used to log the applied rules for the rewriter.
     */
    protected Set<String> appliedRules;

    private InfoLoggingContext infoLoggingContext;

    /**
     * Wrapper for rewriting the query. The logic for logging / debug is completely encapsulated and it will trigger the #rewriteContextAware method.
     *
     * @param query The query to be rewritten
     * @param searchEngineRequestAdapter Encapsulates the request context.
     * @return The rewritten query.
     */
    public ExpandedQuery rewrite(final ExpandedQuery query, final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        this.searchEngineRequestAdapter = searchEngineRequestAdapter;
        this.infoLoggingContext = searchEngineRequestAdapter.getInfoLoggingContext().orElse(null);

        appliedRules = isInfoLogging() ? new HashSet<>() : null;

        final ExpandedQuery expandedQuery = rewriteContextAware(query);

        if (isInfoLogging() && !appliedRules.isEmpty()) {
            final Map<String, Set<String>> message = new IdentityHashMap<>(1);
            message.put(APPLIED_RULES, appliedRules);
            infoLoggingContext.log(message);
        }

        return expandedQuery;
    }

    /**
     * Get or initialize the the debug info if it does not exist.
     */
    protected List<String> getDebugInfo() {
        final List<String> debugInfo = (List<String>) searchEngineRequestAdapter.getContext()
                .getOrDefault(CONTEXT_KEY_DEBUG_DATA, new LinkedList<>());
        // prepare debug info context object if requested
        if (isDebug() && debugInfo.isEmpty()) {
            searchEngineRequestAdapter.getContext().put(CONTEXT_KEY_DEBUG_DATA, debugInfo);
        }

        return debugInfo;
    }

    /**
     * Check for an activated debug request.
     */
    protected boolean isDebug() {
        return Boolean.TRUE.equals(searchEngineRequestAdapter.getContext()
                .get(CONTEXT_KEY_DEBUG_ENABLED));
    }

    /**
     * Check for a valid (enabled) info logging.
     */
    protected boolean isInfoLogging() {
        return infoLoggingContext != null && infoLoggingContext.isEnabledForRewriter();
    }

    /**
     * Rewrite the query. The caller of this method should expect that the query that was passed as an argument to this
     * method could be modified.
     *
     * @param query The query to be rewritten
     * @return The rewritten query.
     *
     */
    abstract public ExpandedQuery rewriteContextAware(ExpandedQuery query);


}
