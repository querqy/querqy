package querqy.solr;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import querqy.infologging.InfoLogging;
import querqy.lucene.rewrite.cache.TermQueryCache;

public class JsonQuerqyDismaxQParserPlugin extends AbstractQuerqyDismaxQParserPlugin {

   @Override
   public QParser createParser(final String qstr, final SolrParams localParams, final SolrParams params,
                               final SolrQueryRequest req, final InfoLogging infoLogging,
                               final TermQueryCache termQueryCache) {
         return new QuerqyJsonQParser(qstr, localParams, params, req,
                 createQuerqyParser(qstr, localParams, params, req), rewriteChain, infoLogging, termQueryCache);
   }
}
