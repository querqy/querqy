package querqy.rewrite;

import querqy.infologging.InfoLoggingContext;

import java.util.Map;
import java.util.Optional;

/**
 * A SearchEngineRequestAdapter is mainly used to pass context-specific information to
 * {@link querqy.rewrite.QueryRewriter}s while hiding search engine specifics from Querqy core.
 *
 * @see querqy.rewrite.ContextAwareQueryRewriter
 *
 */
public interface SearchEngineRequestAdapter {
    /**
     * <p>Get the rewrite chain to be applied to the user query.</p>
     *
     * @return The rewrite chain.
     */
    RewriteChain getRewriteChain();

    /**
     * <p>Get a map to hold context information while rewriting the query.</p>
     *
     * @see querqy.rewrite.ContextAwareQueryRewriter
     * @return A non-null context map.
     */
    Map<String, Object> getContext();

    /**
     * Get request parameter as String
     *
     * @param name the parameter name
     * @return the optional parameter value
     */
    Optional<String> getRequestParam(String name);

    /**
     * Get request parameter as an array of Strings
     *
     * @param name the parameter name
     * @return the parameter value String array (String[0] if not set)
     */
    String[] getRequestParams(String name);

    /**
     * Get request parameter as Boolean
     *
     * @param name the parameter name
     * @return the optional parameter value
     */
    Optional<Boolean> getBooleanRequestParam(String name);

    /**
     * Get request parameter as Integer
     *
     * @param name the parameter name
     * @return the optional parameter value
     */
    Optional<Integer> getIntegerRequestParam(String name);

    /**
     * Get request parameter as Float
     *
     * @param name the parameter name
     * @return the optional parameter value
     */
    Optional<Float> getFloatRequestParam(String name);

    /**
     * Get request parameter as Double
     *
     * @param name the parameter name
     * @return the optional parameter value
     */
    Optional<Double> getDoubleRequestParam(String name);

    /**
     * <p>Get the per-request info logging. Return an empty option if logging hasn't been configured or was disabled
     * for this request.</p>
     *
     * @return the InfoLoggingContext object
     */
    Optional<InfoLoggingContext> getInfoLoggingContext();

    /**
     * <p>Should debug information be collected while rewriting the query?</p>
     * <p>Debug information will be kept in the context map under the
     * {@link querqy.rewrite.AbstractLoggingRewriter#CONTEXT_KEY_DEBUG_DATA} key.</p>
     *
     * @see #getContext()
     *
     * @return true if debug information shall be collected, false otherwise
     */
    boolean isDebugQuery();

}
