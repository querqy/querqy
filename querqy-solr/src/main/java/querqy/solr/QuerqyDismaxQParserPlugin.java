/**
 * 
 */
package querqy.solr;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;

import querqy.parser.QuerqyParser;

/**
 * @author rene
 *
 */
public class QuerqyDismaxQParserPlugin extends AbstractQuergyDismaxQParserPlugin {

    /* (non-Javadoc)
     * @see org.apache.solr.search.QParserPlugin#createParser(java.lang.String, org.apache.solr.common.params.SolrParams, org.apache.solr.common.params.SolrParams, org.apache.solr.request.SolrQueryRequest)
     */
    @Override
    public QParser createParser(String qstr, SolrParams localParams, SolrParams params,
            SolrQueryRequest req) {
    	
    	QuerqyParser querqyParser = createQuerqyParser(qstr, localParams, params, req);
    	
		return new QuerqyDismaxQParser(qstr, localParams, params, req, rewriteChain, new SolrIndexStats(req.getSearcher()), querqyParser);
		
    }


    public QuerqyParser createQuerqyParser(String qstr, SolrParams localParams, SolrParams params,
            SolrQueryRequest req) {
    	try {
    		return querqyParserClass.newInstance();
    	} catch (InstantiationException|IllegalAccessException e) {
    		throw new RuntimeException("Could not create QuerqyParser", e);
    	} 
    }

}
