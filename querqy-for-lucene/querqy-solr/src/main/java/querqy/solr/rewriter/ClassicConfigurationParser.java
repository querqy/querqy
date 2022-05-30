package querqy.solr.rewriter;

import org.apache.lucene.util.ResourceLoader;
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
@Deprecated
public interface ClassicConfigurationParser {

    default Map<String, Object> parseConfigurationToRequestHandlerBody(final NamedList<Object> configuration,
                                                                       final ResourceLoader resourceLoader) {
        final Map<String, Object> result = new HashMap<>();
        if (configuration != null) {
            final Map<String, Object> origConf = configuration.asShallowMap();
            final Map<String, Object> conf = new HashMap<>();
            for (final Map.Entry<String, Object> entry : origConf.entrySet()) {
               final int dotIndex = entry.getKey().indexOf(".");
               if (dotIndex > 0) {
                   final String k = entry.getKey().substring(0, dotIndex);
                   final String subkey = entry.getKey().substring(dotIndex+1);
                   final Map<String, Object> nestedConf = (Map<String, Object>) conf.computeIfAbsent(k, x -> new HashMap<String, Object>());
                   nestedConf.put(subkey, entry.getValue());
               } else {
                   conf.put(entry.getKey(), entry.getValue());
               }
            }
            result.put(CONF_CONFIG, conf);
            ifNotNull(configuration.get(CONF_CLASS), v -> result.put(CONF_CLASS, v));
        }

        return result;
    }

}
