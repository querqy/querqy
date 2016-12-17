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
import querqy.parser.QuerqyParserFactory;

/**
 * A factory for a {@link QuerqyParser}. Unlike the Solr-independent {@link QuerqyParserFactory} the arguments of the
 * {@link #init(NamedList, ResourceLoader)} and {@link #createParser(String, SolrParams, SolrParams, SolrQueryRequest)}
 * methods are part of the Solr object model.
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
