package querqy.solr;

import static org.apache.solr.common.SolrException.ErrorCode.*;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.ExtendedQuery;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.search.WrappedQuery;
import querqy.lucene.LuceneQueries;
import querqy.lucene.QueryParsingController;
import querqy.lucene.SearchEngineRequestAdapter;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.parser.QuerqyParser;
import querqy.rewrite.ContextAwareQueryRewriter;
import querqy.rewrite.RewriteChain;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class QuerqyDismaxQParser extends QParser {

    public static final String MATCH_ALL_QUERY_STRING = "*:*";

    protected final QueryParsingController controller;
    protected final DismaxSearchEngineRequestAdapter requestAdapter;

    protected LuceneQueries luceneQueries = null;
    protected Query processedQuery = null;
    protected final QuerqyParser querqyParser;

    protected final String userQueryString;

    private static final Query MATCH_ALL_QUERY = new MatchAllDocsQuery();
    private static final LuceneQueries MATCH_ALL_LUCENE_QUERIES = new LuceneQueries(MATCH_ALL_QUERY,
            Collections.emptyList(), MATCH_ALL_QUERY, true);

    /**
     * Constructor for the QParser
     *
     * @param qstr        The part of the query string specific to this parser
     * @param localParams The set of parameters that are specific to this QParser.  See http://wiki.apache.org/solr/LocalParams
     * @param params      The rest of the {@link SolrParams}
     * @param req         The original {@link SolrQueryRequest}.
     */
    public QuerqyDismaxQParser(final String qstr, final SolrParams localParams, final SolrParams params,
                                final SolrQueryRequest req, final QuerqyParser querqyParser,
                                final RewriteChain rewriteChain, final TermQueryCache termQueryCache) {
        super(qstr, localParams, params, req);
        final String q = Objects.requireNonNull(qstr).trim();
        if (q.isEmpty()) {
            throw new SolrException(BAD_REQUEST, "Query string must not be empty");
        }
        this.userQueryString = q;
        this.querqyParser = querqyParser;

        requestAdapter = new DismaxSearchEngineRequestAdapter(this, req, userQueryString,
                SolrParams.wrapDefaults(localParams, params), querqyParser, rewriteChain, termQueryCache);


        controller = new QueryParsingController(requestAdapter);

    }


    @Override
    public Query parse() throws SyntaxError {

        try {

            luceneQueries = controller.process();

            if (luceneQueries.querqyBoostQueries != null && luceneQueries.querqyBoostQueries.size() > 0) {
                final BooleanQuery.Builder builder = new BooleanQuery.Builder();

                for (final Query q : luceneQueries.querqyBoostQueries) {
                    builder.add(q, BooleanClause.Occur.SHOULD);
                }

                processedQuery = new QuerqyReRankQuery(maybeWrapQuery(luceneQueries.mainQuery),
                        maybeWrapQuery(builder.build()), requestAdapter.getReRankNumDocs(), 1.0);

            } else {
                processedQuery = maybeWrapQuery(luceneQueries.mainQuery);
            }


        } catch (final SearchEngineRequestAdapter.SyntaxException e) {
            throw new SyntaxError("Syntax error", e);
        }

        return processedQuery;

    }

    @Override
    public Query getQuery() throws SyntaxError {
        if (query==null) {
            query=parse();

            if (localParams != null) {
                final String cacheStr = localParams.get(CommonParams.CACHE);
                if (cacheStr != null) {
                    if (CommonParams.FALSE.equals(cacheStr)) {
                        extendedQuery().setCache(false);
                    } else if (CommonParams.TRUE.equals(cacheStr)) {
                        extendedQuery().setCache(true);
                    } else if ("sep".equals(cacheStr) && !luceneQueries.areQueriesInterdependent) {
                        extendedQuery().setCacheSep(true);
                    }
                }

                int cost = localParams.getInt(CommonParams.COST, Integer.MIN_VALUE);
                if (cost != Integer.MIN_VALUE) {
                    extendedQuery().setCost(cost);
                }
            }
        }
        return query;
    }

    private ExtendedQuery extendedQuery() {
        if (query instanceof ExtendedQuery) {
            return (ExtendedQuery) query;
        } else {
            WrappedQuery wq = new WrappedQuery(query);
            wq.setCacheSep(!luceneQueries.areQueriesInterdependent);
            query = wq;
            return wq;
        }
    }

    protected Query maybeWrapQuery(final Query query) {
        if (!luceneQueries.areQueriesInterdependent) {
            if (query instanceof ExtendedQuery) {
                ((ExtendedQuery) query).setCacheSep(false);
                return query;
            } else {
                final WrappedQuery wrappedQuery = new WrappedQuery(query);
                wrappedQuery.setCacheSep(false);
                return wrappedQuery;
            }
        } else {
            return query;
        }
    }

    @Override
    public Query getHighlightQuery() throws SyntaxError {
        if (processedQuery == null) {
            parse();
        }
        return luceneQueries.userQuery;
    }

    @Override
    public void addDebugInfo(final NamedList<Object> debugInfo) {

        super.addDebugInfo(debugInfo);

        debugInfo.add("querqy.parser", querqyParser.getClass().getName());

        final Map<String, Object> context = requestAdapter.getContext();

        if (context != null) {

            @SuppressWarnings("unchecked") final List<String> rulesDebugInfo =
                    (List<String>) context.get(ContextAwareQueryRewriter.CONTEXT_KEY_DEBUG_DATA);

            if (rulesDebugInfo != null) {
                debugInfo.add("querqy.rewrite", rulesDebugInfo);
            }

        }

    }

    public Map<String, Object> getContext() {
        return requestAdapter.getContext();
    }

    public List<Query> getFilterQueries() {
        return luceneQueries == null ? null : luceneQueries.filterQueries;
    }
}
