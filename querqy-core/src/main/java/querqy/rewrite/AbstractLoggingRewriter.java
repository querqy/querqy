package querqy.rewrite;

import querqy.infologging.InfoLoggingContext;
import querqy.model.AbstractNodeVisitor;
import querqy.model.ExpandedQuery;
import querqy.model.Node;
import querqy.model.rewriting.RewriterOutput;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public abstract class AbstractLoggingRewriter extends AbstractNodeVisitor<Node> {

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

    /**
     * Wrapper for rewriting the query. The logic for logging / debug is completely encapsulated and it will trigger the #rewriteContextAware method.
     *
     * @param query The query to be rewritten
     * @param searchEngineRequestAdapter Encapsulates the request context.
     * @return The rewritten query.
     */
    public RewriterOutput rewrite(final ExpandedQuery query, final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        final boolean isInfoLogging = isInfoLogging(searchEngineRequestAdapter);

        final Set<String> appliedRules = isInfoLogging ? new HashSet<>() : null;

        final RewriterOutput rewrittenQuery = rewrite(query, searchEngineRequestAdapter, appliedRules);

        if (isInfoLogging && !appliedRules.isEmpty()) {
            final Map<String, Set<String>> message = new IdentityHashMap<>(1);
            message.put(APPLIED_RULES, appliedRules);
            searchEngineRequestAdapter.getInfoLoggingContext().ifPresent(context -> context.log(message));
        }

        return rewrittenQuery;
    }

    /**
     * Get or initialize the the debug info if it does not exist.
     *
     * @param searchEngineRequestAdapter Access to request context
     * @return A non-null list of debug messages
     */
    protected List<String> getDebugInfo(final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        final List<String> debugInfo = (List<String>) searchEngineRequestAdapter.getContext()
                .getOrDefault(CONTEXT_KEY_DEBUG_DATA, new LinkedList<>());
        // prepare debug info context object if requested
        if (isDebug(searchEngineRequestAdapter) && debugInfo.isEmpty()) {
            searchEngineRequestAdapter.getContext().put(CONTEXT_KEY_DEBUG_DATA, debugInfo);
        }

        return debugInfo;
    }

    /**
     * Check for an activated debug request.
     *
     * @param searchEngineRequestAdapter Access to request context
     * @return true iff debug is enabled
     */
    protected boolean isDebug(final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return Boolean.TRUE.equals(searchEngineRequestAdapter.getContext()
                .get(CONTEXT_KEY_DEBUG_ENABLED));
    }

    /**
     * Check for a valid (enabled) info logging.
     *
     * @param searchEngineRequestAdapter Access to request context
     * @return true iff info logging is enabled
     */
    protected boolean isInfoLogging(final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        final InfoLoggingContext infoLoggingContext = searchEngineRequestAdapter.getInfoLoggingContext().orElse(null);
        return infoLoggingContext != null && infoLoggingContext.isEnabledForRewriter();
    }

    /**
     * Rewrite the query. The caller of this method should expect that the query that was passed as an argument to this
     * method could be modified.
     *
     * @param query The query to be rewritten
     * @param searchEngineRequestAdapter Access to request context
     * @param infoLogMessages If not null, append info logging information to this set
     * @return The rewritten query.
     *
     */
    abstract public RewriterOutput rewrite(ExpandedQuery query,
                                           final SearchEngineRequestAdapter searchEngineRequestAdapter,
                                           Set<String> infoLogMessages);
}
