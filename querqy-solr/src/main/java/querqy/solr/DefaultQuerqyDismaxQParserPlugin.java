package querqy.solr;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;

public class DefaultQuerqyDismaxQParserPlugin extends AbstractQuerqyDismaxQParserPlugin {


   @Override
   public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
         try {
			return new QuerqyDismaxQParser(qstr, localParams, params, req, rewriteChain,
			       new SolrIndexStats(req.getSearcher()), createQuerqyParser(qstr, localParams, params, req));
		} catch (SyntaxError e) {
			throw new RuntimeException(e);
		}
   }

}
