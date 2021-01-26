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
import org.apache.solr.search.RankQuery;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.search.WrappedQuery;
import querqy.lucene.LuceneQueries;
import querqy.lucene.QueryParsingController;
import querqy.lucene.LuceneSearchEngineRequestAdapter;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.parser.QuerqyParser;
import querqy.rewrite.RewriteChain;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.infologging.InfoLogging;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class QuerqyDismaxQParser extends QParser {

    protected final QueryParsingController controller;
    protected final DismaxSearchEngineRequestAdapter requestAdapter;

    protected LuceneQueries luceneQueries = null;
    protected Query processedQuery = null;
    protected final QuerqyParser querqyParser;

    protected final String userQueryString;

    /**
     * Constructor for the QParser
     *
     * @param qstr        The part of the query string specific to this parser
     * @param localParams The set of parameters that are specific to this QParser.  See http://wiki.apache.org/solr/LocalParams
     * @param params      The rest of the {@link SolrParams}
     * @param req         The original {@link SolrQueryRequest}
     * @param querqyParser The Querqy query parser to be applied to the input query string
     * @param rewriteChain The chain of rewriters to be applied to this request
     * @param infoLogging The info logging object for this request
     * @param termQueryCache The term query cache.
     *
     */
    public QuerqyDismaxQParser(final String qstr, final SolrParams localParams, final SolrParams params,
                               final SolrQueryRequest req, final QuerqyParser querqyParser,
                               final RewriteChain rewriteChain, final InfoLogging infoLogging,
                               final TermQueryCache termQueryCache) {
        super(qstr, localParams, params, req);
        final String q = Objects.requireNonNull(qstr).trim();

        if (q.isEmpty()) {
            throw new SolrException(BAD_REQUEST, "Query string must not be empty");
        }

        this.userQueryString = q;
        this.querqyParser = querqyParser;

        requestAdapter = new DismaxSearchEngineRequestAdapter(this, req, userQueryString,
                SolrParams.wrapDefaults(localParams, params), querqyParser, rewriteChain, infoLogging, termQueryCache);


        controller = createQueryParsingController();

    }

    public QueryParsingController createQueryParsingController() {
        return new QueryParsingController(requestAdapter);
    }


    @Override
    public Query parse() throws SyntaxError {

        try {

            luceneQueries = controller.process();

            processedQuery = maybeWrapQuery(luceneQueries.mainQuery);

        } catch (final LuceneSearchEngineRequestAdapter.SyntaxException e) {
            throw new SyntaxError("Syntax error", e);
        }

        return processedQuery;

    }

    @Override
    public Query getQuery() throws SyntaxError {
        if (query==null) {
            query=parse();
            applyLocalParams();

        }
        return query;
    }

    protected void applyLocalParams() {

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
        final Map<String, Object> info = controller.getDebugInfo();
        for (final Map.Entry<String, Object> entry : info.entrySet()) {
            debugInfo.add(entry.getKey(), entry.getValue());
        }

    }

      public SearchEngineRequestAdapter getSearchEngineRequestAdapter() {
        return requestAdapter;
    }

    public List<Query> getFilterQueries() {
        return luceneQueries == null ? null : luceneQueries.filterQueries;
    }

    public Optional<RankQuery> getRankQuery() {
        // there are two cases this QParser returns a RankQuery here:
        //   1) the parsed query contains boosts and querqy.solr.QuerqyDismaxParams.QBOOST_METHOD is set to "rerank", or
        //   2) a rank query was supplied via parameter querqy.rq and there is no boosting already on the main query
        if (luceneQueries.querqyBoostQueries != null && luceneQueries.querqyBoostQueries.size() > 0) {
            final BooleanQuery.Builder builder = new BooleanQuery.Builder();

            for (final Query q : luceneQueries.querqyBoostQueries) {
                builder.add(q, BooleanClause.Occur.SHOULD);
            }

            return Optional.of(new QuerqyReRankQuery(new MatchAllDocsQuery(), maybeWrapQuery(builder.build()),
                    requestAdapter.getReRankNumDocs(), 1.0));

        } else if (luceneQueries.rankQuery != null && !luceneQueries.isMainQueryBoosted) {
            // an external rank query (parsed from querqy.rq) is only applied if no querqy boost queries have been applied
            // (either by applying them on the main query as optional clauses or by wrapping the main query in a QuerqyReRankQuery)
            return Optional.of((RankQuery) luceneQueries.rankQuery);
        }

        return Optional.empty();
    }
}
