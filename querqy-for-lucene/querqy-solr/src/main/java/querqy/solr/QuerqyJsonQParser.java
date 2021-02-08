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

import static org.apache.solr.common.SolrException.ErrorCode.BAD_REQUEST;

public class QuerqyJsonQParser extends QuerqyDismaxQParser {

    private static final String FIELD_QUERY = "query";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_DEFAULT_TYPE = "defType";

    public QuerqyJsonQParser(final String qstr, final SolrParams localParams, final SolrParams params,
                             final SolrQueryRequest req, final QuerqyParser querqyParser,
                             final RewriteChain rewriteChain, final InfoLogging infoLogging,
                             final TermQueryCache termQueryCache) {
        super(qstr, localParams, params, req, querqyParser, rewriteChain, infoLogging, termQueryCache);
    }

    public String getQueryParserName() {
        if (super.localParams != null) {
            final String queryParser = super.localParams.get(FIELD_TYPE);

            if (queryParser != null) {
                return queryParser;
            }

        } else if (super.params != null) {
            final String queryParser = super.params.get(FIELD_TYPE);
            if (queryParser != null) {
                return queryParser;
            }

            final String defaultQueryParser = super.params.get(FIELD_DEFAULT_TYPE);
            if (defaultQueryParser != null) {
                return defaultQueryParser;
            }
        }

        return null;
    }

    @Override
    public QueryParsingController createQueryParsingController() {

        final Object solrQueryObj = req.getJSON().get(FIELD_QUERY);
        if (solrQueryObj == null) {
            throw new SolrException(BAD_REQUEST, "Solr query not defined");
        }

        final Map solrQuery = asMap(solrQueryObj);

        final String queryType = getQueryParserName();
        final Object querqyRequestObj = solrQuery.get(queryType);

        if (querqyRequestObj == null) {
            throw new SolrException(BAD_REQUEST, "Could not find request object in JSON for query type: " + queryType);
        }

        final Map querqyRequest = asMap(querqyRequestObj);

        final Object querqyQueryObj = querqyRequest.get(FIELD_QUERY);

        if (querqyQueryObj == null) {
            throw new SolrException(BAD_REQUEST, "Missing query for querqy request");
        }

        final Map querqyQuery = asMap(querqyQueryObj);

        return new JsonQueryParsingController(querqyQuery, requestAdapter);
    }

    private Map asMap(final Object obj) {
        if (obj instanceof Map) {
            return (Map) obj;

        } else {
            throw new SolrException(BAD_REQUEST, String.format("Element %s is expected to be of type Map", obj.toString()));
        }
    }
}
