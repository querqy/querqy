package querqy.solr.rewriter;

import org.apache.solr.common.SolrException;
import querqy.rewrite.RewriterFactory;
import querqy.solr.RewriterConfigRequestBuilder;
import querqy.solr.SolrRewriterFactoryAdapter;
import querqy.solr.utils.ConfigUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RewriterFactoryLoader for {@link NumberQueryRewriterFactory}
 */
public class NumberQueryRewriterFactory extends SolrRewriterFactoryAdapter implements ClassicConfigurationParser {

    public static final String CONF_ACCEPT_GENERATED_TERMS = "acceptGeneratedTerms";

    querqy.rewrite.contrib.NumberQueryRewriterFactory factory = null;

    public NumberQueryRewriterFactory(final String rewriterId) {
        super(rewriterId);
    }

    @Override
    public void configure(final Map<String, Object> config) throws SolrException {

        factory = ConfigUtils.getBoolArg(config, CONF_ACCEPT_GENERATED_TERMS)
                .map(accept -> new querqy.rewrite.contrib.NumberQueryRewriterFactory(rewriterId, accept))
                .orElseGet(() -> new querqy.rewrite.contrib.NumberQueryRewriterFactory(rewriterId));

    }

    @Override
    public List<String> validateConfiguration(final Map<String, Object> config) {
        try {
            ConfigUtils.getBoolArg(config, CONF_ACCEPT_GENERATED_TERMS);
            return null;
        } catch (final Exception e){
            return Collections.singletonList("boolean value expected for " + CONF_ACCEPT_GENERATED_TERMS);
        }

    }

    @Override
    public RewriterFactory getRewriterFactory() {
        return factory;
    }

    public static class NumberQueryConfigRequestBuilder extends RewriterConfigRequestBuilder {

        private Boolean acceptGeneratedTerms;

        public NumberQueryConfigRequestBuilder() {
            super(NumberQueryRewriterFactory.class);
        }

        public NumberQueryConfigRequestBuilder(final boolean acceptGeneratedTerms) {
            super(NumberQueryRewriterFactory.class);
            this.acceptGeneratedTerms = acceptGeneratedTerms;
        }

        @Override
        public Map<String, Object> buildConfig() {
            if (acceptGeneratedTerms == null) {
                return Collections.emptyMap();
            } else {
                final Map<String, Object> config = new HashMap<>(1);
                config.put(CONF_ACCEPT_GENERATED_TERMS, acceptGeneratedTerms);
                return config;
            }
        }

        public NumberQueryConfigRequestBuilder acceptGeneratedTerms(final Boolean accept) {
            acceptGeneratedTerms = accept;
            return this;
        }

    }
}
