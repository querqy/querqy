package querqy.infologging;

import querqy.rewrite.SearchEngineRequestAdapter;

public interface Sink {

    void log(Object message, String rewriterId, SearchEngineRequestAdapter searchEngineRequestAdapter);
    void endOfRequest(SearchEngineRequestAdapter searchEngineRequestAdapter);


}
