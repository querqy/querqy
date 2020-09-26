package querqy.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.SolrCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import querqy.lucene.rewrite.cache.CacheKey;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.lucene.rewrite.cache.TermQueryCacheValue;
import querqy.parser.QuerqyParser;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.RewriteChain;
import querqy.rewrite.RewriterFactory;
import querqy.infologging.InfoLogging;
import querqy.infologging.Sink;
import querqy.lucene.GZIPAwareResourceLoader;

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
