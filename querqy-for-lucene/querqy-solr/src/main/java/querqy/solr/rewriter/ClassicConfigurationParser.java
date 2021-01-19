package querqy.solr.rewriter;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrResourceLoader;

import java.io.IOException;
import java.util.Map;

public interface ClassicConfigurationParser {

    Map<String, Object> parseConfiguration(NamedList<?> configuration, SolrResourceLoader resourceLoader) throws IOException;

}
