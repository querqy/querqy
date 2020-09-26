package querqy.solr;

import org.apache.solr.common.SolrException;
import org.apache.solr.core.SolrResourceLoader;
import querqy.rewrite.RewriterFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class SolrRewriterFactoryAdapter {

    protected final String rewriterId;

    public SolrRewriterFactoryAdapter(final String rewriterId) {
        this.rewriterId = rewriterId;
    }

    public abstract void configure(Map<String, Object> config);

    public abstract List<String> validateConfiguration(Map<String, Object> config);

    public abstract RewriterFactory getRewriterFactory();

    public String getRewriterId() {
        return rewriterId;
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

        return loadInstance(rewriterId, className);

    }

    public static SolrRewriterFactoryAdapter loadInstance(final String rewriterId, final String className) {

        try {
            return (SolrRewriterFactoryAdapter) Class.forName(className).getDeclaredConstructor(String.class)
                    .newInstance(rewriterId);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

    }

}
