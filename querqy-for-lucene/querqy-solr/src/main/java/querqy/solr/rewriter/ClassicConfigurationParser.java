package querqy.solr.rewriter;

import org.apache.solr.common.util.NamedList;
import querqy.lucene.GZIPAwareResourceLoader;

import java.util.Map;

public interface ClassicConfigurationParser {

    Map<String, Object> parseConfigurationToRequestHandlerBody(NamedList<?> configuration, GZIPAwareResourceLoader resourceLoader) throws RuntimeException;

}
