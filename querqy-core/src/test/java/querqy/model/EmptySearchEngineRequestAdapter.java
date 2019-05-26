package querqy.model;

import querqy.infologging.InfoLogging;
import querqy.rewrite.RewriteChain;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.infologging.InfoLoggingContext;

import java.util.*;

public class EmptySearchEngineRequestAdapter implements SearchEngineRequestAdapter {

    Map<String, Object> context = new HashMap<>();

    InfoLoggingContext loggingContext = new InfoLoggingContext(new InfoLogging(Collections.emptyMap()), this);

    @Override
    public RewriteChain getRewriteChain() {
        return null;
    }

    @Override
    public Map<String, Object> getContext() {
        return context;
    }

    @Override
    public Optional<String> getRequestParam(String name) {
        return Optional.empty();
    }

    @Override
    public String[] getRequestParams(String name) {
        return new String[0];
    }

    @Override
    public Optional<Boolean> getBooleanRequestParam(String name) {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getIntegerRequestParam(String name) {
        return Optional.empty();
    }

    @Override
    public Optional<Float> getFloatRequestParam(String name) {
        return Optional.empty();
    }

    @Override
    public Optional<Double> getDoubleRequestParam(String name) {
        return Optional.empty();
    }

    @Override
    public boolean isDebugQuery() {
        return false;
    }

    @Override
    public Optional<InfoLoggingContext> getInfoLoggingContext() {
        return Optional.ofNullable(loggingContext);
    }
}