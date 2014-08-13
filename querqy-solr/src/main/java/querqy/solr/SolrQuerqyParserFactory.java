/**
 * 
 */
package querqy.solr;

import java.io.IOException;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;

import querqy.parser.QuerqyParser;

/**
 * @author René Kriegler, @renekrie
 *
 */
public interface SolrQuerqyParserFactory {
	
	void init(@SuppressWarnings("rawtypes") NamedList parserConfig, ResourceLoader loader) throws IOException, SolrException;
	
	QuerqyParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req);

}
