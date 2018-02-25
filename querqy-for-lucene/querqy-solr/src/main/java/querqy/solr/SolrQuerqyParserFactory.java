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
 * A factory that creates a {@link QuerqyParser} per Solr query request. The QuerqyParser turns the query string
 * into Querqy's internal query object model.
 * 
 * @author René Kriegler, @renekrie
 *
 */
public interface SolrQuerqyParserFactory {

	/**
	 * Initialize this factory with configuration parameters.
	 * 
	 * @param parserConfig
	 * @param loader
	 * @throws IOException
	 * @throws SolrException
	 */
    void init(@SuppressWarnings("rawtypes") NamedList parserConfig, ResourceLoader loader) throws IOException,
         SolrException;

    /**
     * 
     * Create a {@link QuerqyParser} for the given request context.
     * 
     * @param qstr
     * @param localParams
     * @param params
     * @param req
     * @return
     */
    QuerqyParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req);

}
