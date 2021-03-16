package querqy.infologging;

import querqy.rewrite.SearchEngineRequestAdapter;

/**
 * Per-request access to InfoLogging
 */
public class InfoLoggingContext {

    private final InfoLogging infoLogging;
    private final SearchEngineRequestAdapter searchEngineRequestAdapter;

    private String rewriterId = null;

    public InfoLoggingContext(final InfoLogging infoLogging,
                              final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        this.infoLogging = infoLogging;
        this.searchEngineRequestAdapter = searchEngineRequestAdapter;
    }

    public String getRewriterId() {
        return rewriterId;
    }

    public void setRewriterId(final String rewriterId) {
        this.rewriterId = rewriterId;
    }

    public void log(final Object message) {
        infoLogging.log(message, rewriterId, searchEngineRequestAdapter);
    }

    public void endOfRequest() {
        infoLogging.endOfRequest(searchEngineRequestAdapter);
    }

    public boolean isEnabledForRewriter() {
        return infoLogging.isLoggingEnabledForRewriter(rewriterId);
    }



}
