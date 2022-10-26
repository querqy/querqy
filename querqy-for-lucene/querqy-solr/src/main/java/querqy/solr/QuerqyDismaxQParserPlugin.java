package querqy.solr;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.lucene.rewrite.infologging.InfoLogging;
import querqy.rewrite.RewriteChain;

/**
 * Querqy's default QParserPlugin. The produced QParser combines query rewriting with (e)dismax query QParserPlugin
 * features.
 */
public class QuerqyDismaxQParserPlugin extends QuerqyQParserPlugin {

    public QParser createParser(final String qstr, final SolrParams localParams, final SolrParams params,
                                final SolrQueryRequest req, final RewriteChain rewriteChain,
                                final InfoLogging infoLogging, final TermQueryCache termQueryCache) {
        return new QuerqyDismaxQParser(qstr, localParams, params, req,
                createQuerqyParser(qstr, localParams, params, req), rewriteChain, infoLogging, termQueryCache);
    }


}
