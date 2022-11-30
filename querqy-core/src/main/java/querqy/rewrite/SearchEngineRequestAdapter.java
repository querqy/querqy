package querqy.rewrite;

import java.util.Map;
import java.util.Optional;

/**
 * A SearchEngineRequestAdapter is mainly used to pass context-specific information to
 * {@link querqy.rewrite.QueryRewriter}s while hiding search engine specifics from Querqy core.
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
     * @return true if debug information shall be collected, false otherwise
     */
    boolean isDebugQuery();

    RewriteLoggingConfig getRewriteLoggingConfig();

}
