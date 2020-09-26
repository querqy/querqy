package querqy.solr.rewriter;

import org.apache.solr.common.SolrException;

import querqy.rewrite.RewriterFactory;
import querqy.solr.SolrRewriterFactoryAdapter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * RewriterFactoryLoader for {@link ShingleRewriterFactory}
 */
public class ShingleRewriterFactory extends SolrRewriterFactoryAdapter {

    querqy.rewrite.contrib.ShingleRewriterFactory factory = null;

    public ShingleRewriterFactory(final String rewriterId) {
        super(rewriterId);
    }

    @Override
    public void configure(final Map<String, Object> config)
            throws SolrException {
        final Object acceptGeneratedTerms = config.get("acceptGeneratedTerms");

        if (acceptGeneratedTerms == null) {
            factory = new querqy.rewrite.contrib.ShingleRewriterFactory(rewriterId);
        } else if (acceptGeneratedTerms instanceof Boolean) {
            factory = new querqy.rewrite.contrib.ShingleRewriterFactory(rewriterId, (Boolean) acceptGeneratedTerms);
        } else {
            factory = new querqy.rewrite.contrib.ShingleRewriterFactory(rewriterId, Boolean.parseBoolean(
                    acceptGeneratedTerms.toString()));
        }

    }

    @Override
    public List<String> validateConfiguration(final Map<String, Object> config) {
        final Object acceptGeneratedTerms = config.get("acceptGeneratedTerms");
        if ((acceptGeneratedTerms == null) || (acceptGeneratedTerms instanceof Boolean)) {
            return null;
        }

        try {
            Boolean.parseBoolean(acceptGeneratedTerms.toString());
            return null;
        } catch (final Exception e){
            return Collections.singletonList("boolean value expected for acceptGeneratedTerms");
        }

    }

    @Override
    public RewriterFactory getRewriterFactory() {
        return factory;
    }
}
