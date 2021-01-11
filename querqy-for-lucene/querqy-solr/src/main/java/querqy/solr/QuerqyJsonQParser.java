package querqy.solr;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import querqy.infologging.InfoLogging;
import querqy.lucene.JsonQueryParsingController;
import querqy.lucene.QueryParsingController;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.parser.QuerqyParser;
import querqy.rewrite.RewriteChain;

import java.util.Map;

import static java.util.Objects.isNull;
import static org.apache.solr.common.SolrException.ErrorCode.BAD_REQUEST;

public class QuerqyJsonQParser extends QuerqyDismaxQParser {

    private static final String QUERY_FIELD = "query";
    private static final String DEF_TYPE_FIELD = "type";

    public QuerqyJsonQParser(final String qstr, final SolrParams localParams, final SolrParams params,
                             final SolrQueryRequest req, final QuerqyParser querqyParser,
                             final RewriteChain rewriteChain, final InfoLogging infoLogging,
                             final TermQueryCache termQueryCache) {
        super(qstr, localParams, params, req, querqyParser, rewriteChain, infoLogging, termQueryCache);
    }

    @Override
    public QueryParsingController createQueryParsingController() {

        final Object solrQueryObj = req.getJSON().get(QUERY_FIELD);
        if (isNull(solrQueryObj)) {
            throw new SolrException(BAD_REQUEST, "Solr query not defined");
        }

        final Map solrQuery = castMap(solrQueryObj);

        final String defType = localParams.get(DEF_TYPE_FIELD);
        final Object querqyRequestObj = solrQuery.get(defType);

        if (isNull(querqyRequestObj)) {
            throw new SolrException(BAD_REQUEST, "Defined query parser varies from JSON input");
        }

        final Map querqyRequest = castMap(querqyRequestObj);

        final Object querqyQueryObj = querqyRequest.get(QUERY_FIELD);

        if (isNull(querqyQueryObj)) {
            throw new SolrException(BAD_REQUEST, "Missing query for querqy request");
        }

        final Map querqyQuery = castMap(querqyQueryObj);

        return new JsonQueryParsingController(querqyQuery, requestAdapter);
    }

    private Map castMap(final Object obj) {
        if (obj instanceof Map) {
            return (Map) obj;

        } else {
            throw new SolrException(BAD_REQUEST, String.format("Element %s is expected to be of type Map", obj.toString()));
        }
    }
}
