package querqy.model;

import querqy.rewrite.RewriteChain;
import querqy.rewrite.SearchEngineRequestAdapter;

import java.util.*;

public class EmptySearchEngineRequestAdapter implements SearchEngineRequestAdapter {

    Map<String, Object> context = new HashMap<>();

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

}