package querqy.solr.rewriter;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrResourceLoader;

import java.util.Map;

public interface ClassicConfigurationParser {

    Map<String, Object> parseConfigurationToRequestHandlerBody(NamedList<?> configuration, SolrResourceLoader resourceLoader) throws RuntimeException;

}
