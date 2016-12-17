package querqy.solr;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;

import querqy.lucene.rewrite.cache.TermQueryCache;

public class DefaultQuerqyDismaxQParserPlugin extends AbstractQuerqyDismaxQParserPlugin {

   @Override
   public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req, TermQueryCache termQueryCache) {
      try {
         return new QuerqyDismaxQParser(qstr, localParams, params, req, rewriteChain,
                 createQuerqyParser(qstr, localParams, params, req), termQueryCache);
      } catch (SyntaxError e) {
         throw new RuntimeException(e);
      }
   }

}
