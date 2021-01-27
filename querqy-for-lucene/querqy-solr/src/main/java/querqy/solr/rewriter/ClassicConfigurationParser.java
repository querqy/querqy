package querqy.solr.rewriter;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;
import querqy.solr.SolrRewriterFactoryAdapter;

import java.util.HashMap;
import java.util.Map;

import static querqy.solr.RewriterConfigRequestBuilder.CONF_CLASS;
import static querqy.solr.RewriterConfigRequestBuilder.CONF_CONFIG;
import static querqy.solr.utils.ConfigUtils.ifNotNull;

/**
 * By implementing this interface, a {@link SolrRewriterFactoryAdapter} can also be used in the 'old' / backwards
 * compatible rewriter configuration parsing (solrconfig.xml configuration instead of the REST rules uploading).
 */
public interface ClassicConfigurationParser {

    default Map<String, Object> parseConfigurationToRequestHandlerBody(final NamedList<Object> configuration, final ResourceLoader resourceLoader) throws RuntimeException {
        final Map<String, Object> result = new HashMap<>();
        if (configuration != null) {
            result.put(CONF_CONFIG, configuration.asShallowMap(true));
            ifNotNull(configuration.get(CONF_CLASS), v -> result.put(CONF_CLASS, v));
        }

        return result;
    }

}
