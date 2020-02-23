package querqy.infologging;

import querqy.rewrite.SearchEngineRequestAdapter;

/**
 * Info logging messages can be written to various types of Sinks.
 */
public interface Sink {

    /**
     * <p>Log a message. If the implementation wants to cache the message during the request, it should use the
     * request context via {@link SearchEngineRequestAdapter#getContext()} for caching and flush the output when
     * {@link #endOfRequest(SearchEngineRequestAdapter)} is called.</p>
     * <p>Sink objects can be shared across requests.</p>
     *
     * @param message The message to be logged
     * @param rewriterId The rewriter for which the event should be logged
     * @param searchEngineRequestAdapter Provides the context for collecting the log output
     */
    void log(Object message, String rewriterId, SearchEngineRequestAdapter searchEngineRequestAdapter);

    /**
     * <p>Signals the end of a request. Messages that were cached via the
     * {@link SearchEngineRequestAdapter#getContext()} should now be flushed.</p>
     *
     * @param searchEngineRequestAdapter Provides the context for collecting the log output
     */
    void endOfRequest(SearchEngineRequestAdapter searchEngineRequestAdapter);


}
