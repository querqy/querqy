package querqy.solr.rewriter;

import org.apache.solr.common.SolrException;
import querqy.rewrite.RewriterFactory;
import querqy.solr.RewriterConfigRequestBuilder;
import querqy.solr.SolrRewriterFactoryAdapter;
import querqy.solr.utils.ConfigUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RewriterFactoryLoader for {@link NumberConcatenationRewriterFactory}
 */
public class NumberConcatenationRewriterFactory extends SolrRewriterFactoryAdapter implements ClassicConfigurationParser {

    public static final String CONF_ACCEPT_GENERATED_TERMS = "acceptGeneratedTerms";
    public static final String CONF_MIN_LENGTH_QUERY_TERM = "minimumLengthOfResultingQueryTerm";

    public static  final int DEFAULT_MIN_LENGTH_QUERY_TERM = 3;

    private querqy.rewrite.contrib.NumberConcatenationRewriterFactory factory = null;

    public NumberConcatenationRewriterFactory(final String rewriterId) {
        super(rewriterId);
    }

    @Override
    public void configure(final Map<String, Object> config) throws SolrException {
        final boolean accept = ConfigUtils.getArg(config, CONF_ACCEPT_GENERATED_TERMS, Boolean.FALSE);

        // the maximum length of a combined term
        final Integer minimumLength = ConfigUtils.getArg(config, CONF_MIN_LENGTH_QUERY_TERM,
                DEFAULT_MIN_LENGTH_QUERY_TERM);

        factory = new querqy.rewrite.contrib.NumberConcatenationRewriterFactory(rewriterId, accept, minimumLength);
    }

    @Override
    public List<String> validateConfiguration(final Map<String, Object> config) {
        List<String> errors = new ArrayList<>();

        try {
            ConfigUtils.getBoolArg(config, CONF_ACCEPT_GENERATED_TERMS);
        } catch (final Exception e) {
            errors.add("boolean value expected for " + CONF_ACCEPT_GENERATED_TERMS);
        }

        try {
            ConfigUtils.getIntArg(config, CONF_MIN_LENGTH_QUERY_TERM);
        } catch (final Exception e) {
            errors.add("integer value expected for " + CONF_MIN_LENGTH_QUERY_TERM);
        }

        return errors.isEmpty() ? null : errors;
    }


    @Override
    public RewriterFactory getRewriterFactory() {
        return factory;
    }

    public static class NumberConcatenationConfigRequestBuilder extends RewriterConfigRequestBuilder {

        private Boolean acceptGeneratedTerms;
        private Integer minimumLengthOfResultingQueryTerm;

        public NumberConcatenationConfigRequestBuilder() {
            super(NumberConcatenationRewriterFactory.class);
        }

        @Override
        public Map<String, Object> buildConfig() {
            final Map<String, Object> config = new HashMap<>();
            if (acceptGeneratedTerms != null) {
                config.put(CONF_ACCEPT_GENERATED_TERMS, acceptGeneratedTerms);
            }
            if (minimumLengthOfResultingQueryTerm != null) {
                config.put(CONF_MIN_LENGTH_QUERY_TERM, minimumLengthOfResultingQueryTerm);
            }
            return config;
        }

        public NumberConcatenationConfigRequestBuilder acceptGeneratedTerms(final Boolean accept) {
            acceptGeneratedTerms = accept;
            return this;
        }

        public NumberConcatenationConfigRequestBuilder minimumLengthOfResultingQueryTerm(final Integer minimumLength) {
            minimumLengthOfResultingQueryTerm = minimumLength;
            return this;
        }

    }
}
