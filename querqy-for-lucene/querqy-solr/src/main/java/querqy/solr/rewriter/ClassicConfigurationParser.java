package querqy.solr.rewriter;

import org.apache.solr.common.util.NamedList;
import querqy.lucene.GZIPAwareResourceLoader;
import querqy.solr.SolrRewriterFactoryAdapter;

import java.util.Map;

/**
 * By implementing this interface, a {@link SolrRewriterFactoryAdapter} can also be used in the 'old' / backwards
 * compatible rewriter configuration parsing (solrconfig.xml configuration instead of the REST rules uploading).
 */
public interface ClassicConfigurationParser {

    Map<String, Object> parseConfigurationToRequestHandlerBody(NamedList<?> configuration, GZIPAwareResourceLoader resourceLoader) throws RuntimeException;

}
