package querqy.solr;

import querqy.lucene.rewrite.infologging.Sink;
import querqy.rewrite.RewriterFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class SolrRewriterFactoryAdapter {

    protected final String rewriterId;
    // TODO: add sink here
    private final List<Sink> sinks = new ArrayList<>();

    public SolrRewriterFactoryAdapter(final String rewriterId) {
        this.rewriterId = rewriterId;
    }

    public abstract void configure(Map<String, Object> config);

    public abstract List<String> validateConfiguration(Map<String, Object> config);

    public abstract RewriterFactory getRewriterFactory();

    public String getRewriterId() {
        return rewriterId;
    }

    public List<Sink> getSinks() {
        return sinks;
    }

    public void addSink(final Sink sink) {
        sinks.add(sink);
    }

    public void addSinks(final List<Sink> sinks) {
        this.sinks.addAll(sinks);
    }

    public static SolrRewriterFactoryAdapter loadInstance(final String rewriterId,
                                                          final Map<String, Object> instanceDesc) {

        final String classField = (String) instanceDesc.get("class");
        if (classField == null) {
            throw new IllegalArgumentException("'class' property not found for rewriter configuration : " + rewriterId);
        }

        final String className = classField.trim();
        if (className.isEmpty()) {
            throw new IllegalArgumentException("Class name expected in property 'class'");
        }

        final SolrRewriterFactoryAdapter adapter = loadInstance(rewriterId, className);
        final List<Sink> sinks = loadSinks(instanceDesc);
        if (sinks.isEmpty()) {
            adapter.addSink(ResponseSink.defaultSink());
        }
        adapter.addSinks(sinks);

        return adapter;

    }

    public static SolrRewriterFactoryAdapter loadInstance(final String rewriterId, final String className) {

        try {
            return (SolrRewriterFactoryAdapter) Class.forName(className).getDeclaredConstructor(String.class)
                    .newInstance(rewriterId);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static List<Sink> loadSinks(final Map<String, Object> instanceDesc) {
        final List<Sink> sinks = new ArrayList<>();

        final Map logging = (Map) instanceDesc.get("logging");
        if (logging == null) {
            return sinks;
        }

        final List sinkDefinitions = (List) logging.get("sinks");
        if (sinkDefinitions == null) {
            throw new IllegalArgumentException("Definition of sinks expected in logging");
        }

        for (final Object sink : sinkDefinitions) {
            try {
                sinks.add((Sink) Class.forName((String) sink).getDeclaredConstructor().newInstance());

            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        return sinks;
    }

}
