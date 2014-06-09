/**
 * 
 */
package querqy.solr;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;

/**
 * @author rene
 *
 */
public class QuerqyQParserPlugin extends QParserPlugin {

    /* (non-Javadoc)
     * @see org.apache.solr.util.plugin.NamedListInitializedPlugin#init(org.apache.solr.common.util.NamedList)
     */
    
    @Override
    public void init(@SuppressWarnings("rawtypes") NamedList args) {
    }

    /* (non-Javadoc)
     * @see org.apache.solr.search.QParserPlugin#createParser(java.lang.String, org.apache.solr.common.params.SolrParams, org.apache.solr.common.params.SolrParams, org.apache.solr.request.SolrQueryRequest)
     */
    @Override
    public QParser createParser(String qstr, SolrParams localParams, SolrParams params,
            SolrQueryRequest req) {
        return new QuerqyQParser(qstr, localParams, params, req);
    }

}
