package querqy.infologging;

import querqy.rewrite.SearchEngineRequestAdapter;

public interface InfoLogging {
    void log(Object message, String rewriterId,
             SearchEngineRequestAdapter searchEngineRequestAdapter);

    void endOfRequest(SearchEngineRequestAdapter searchEngineRequestAdapter);

    boolean isLoggingEnabledForRewriter(String rewriterId);
}
