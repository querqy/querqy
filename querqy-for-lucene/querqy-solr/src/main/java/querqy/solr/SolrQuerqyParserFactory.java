/**
 * 
 */
package querqy.solr;

import java.io.IOException;

import org.apache.lucene.util.ResourceLoader;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;

import querqy.parser.QuerqyParser;

/**
 * A factory that creates a {@link QuerqyParser} per Solr query request. The QuerqyParser turns the query string
 * into Querqy's internal query object model.
 * 
 * @author René Kriegler, @renekrie
 *
 */
public interface SolrQuerqyParserFactory {

	/**
	 * Initialise this factory with configuration parameters.
	 * 
	 * @param parserConfig The configuration (normally kept in solrconfig.xml)
	 * @param loader A resource loader for initialising resources when a {@link org.apache.solr.core.SolrCore} is loaded.
	 * @throws IOException if a resource cannot be loaded
	 * @throws SolrException if the SolrQuerqyParserFactory cannot be initialised.
	 */
    void init(@SuppressWarnings("rawtypes") NamedList parserConfig, ResourceLoader loader) throws IOException,
         SolrException;

    /**
     * 
     * Create a {@link QuerqyParser} for the given request context.
     * 
     * @param qstr	The query String
     * @param localParams Local Solr params
     * @param params Solr request params
     * @param req The current Solr request
     * @return A Querqy query parser for qstr
     */
    QuerqyParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req);

}
