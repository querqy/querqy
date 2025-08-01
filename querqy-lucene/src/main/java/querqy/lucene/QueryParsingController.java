package querqy.lucene;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queries.function.FunctionQuery;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.valuesource.ConstValueSource;
import org.apache.lucene.queries.function.valuesource.IfFunction;
import org.apache.lucene.queries.function.valuesource.ProductFloatFunction;
import org.apache.lucene.queries.function.valuesource.QueryValueSource;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import querqy.lucene.LuceneSearchEngineRequestAdapter.SyntaxException;
import querqy.lucene.rewrite.AdditiveBoostFunction;
import querqy.lucene.rewrite.DocumentFrequencyCorrection;
import querqy.lucene.rewrite.LuceneQueryBuilder;
import querqy.lucene.rewrite.LuceneTermQueryBuilder;
import querqy.lucene.rewrite.SearchFieldsAndBoosting;
import querqy.lucene.rewrite.SearchFieldsAndBoosting.FieldBoostModel;
import querqy.lucene.rewrite.TermQueryBuilder;
import querqy.model.BoostQuery;
import querqy.model.ExpandedQuery;
import querqy.model.MatchAllQuery;
import querqy.model.QuerqyQuery;
import querqy.model.RawQuery;
import querqy.rewrite.logging.RewriteChainLog;
import querqy.rewrite.RewriteChainOutput;
import querqy.parser.QuerqyParser;
import querqy.parser.WhiteSpaceQuerqyParser;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by rene on 23/05/2017.
 */
public class QueryParsingController {

    /**
     * The default value for {@link LuceneSearchEngineRequestAdapter#getUserQuerySimilarityScoring()}
     * (= {@link QuerySimilarityScoring#DFC})
     */
    protected static final QuerySimilarityScoring DEFAULT_USER_QUERY_SIMILARITY_SCORING = QuerySimilarityScoring.DFC;

    /**
     * The default value for {@link LuceneSearchEngineRequestAdapter#getBoostQuerySimilarityScoring()}
     * (= {@link QuerySimilarityScoring#DFC})
     */
    protected static final QuerySimilarityScoring DEFAULT_BOOST_QUERY_SIMILARITY_SCORING = QuerySimilarityScoring.DFC;

    protected static final float DEFAULT_TIEBREAKER = 0f;
    protected static final float DEFAULT_MULTI_MATCH_TIEBREAKER = 1f;

    protected static final float DEFAULT_POSITIVE_QUERQY_BOOST_WEIGHT = 1f;
    protected static final float DEFAULT_NEGATIVE_QUERQY_BOOST_WEIGHT = 1f;

    protected static final float DEFAULT_GENERATED_FIELD_BOOST = 1f;

    /**
     * The default field boost model (= {@link FieldBoostModel#FIXED})
     */
    protected static final FieldBoostModel DEFAULT_FIELD_BOOST_MODEL = FieldBoostModel.FIXED;

    /**
     * The default QuerqyParser class for parsing the user query string. (= {@link querqy.parser.WhiteSpaceQuerqyParser})
     */
    protected static final Class<? extends QuerqyParser> DEFAULT_PARSER_CLASS = WhiteSpaceQuerqyParser.class;

    protected static final ObjectMapper REWRITE_LOGGING_OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    protected final LuceneSearchEngineRequestAdapter requestAdapter;
    protected final String queryString;
    protected final boolean needsScores;
    protected final Analyzer queryAnalyzer;
    protected final SearchFieldsAndBoosting searchFieldsAndBoosting;
    protected final DocumentFrequencyCorrection dfc;
    protected final LuceneQueryBuilder builder;
    protected final TermQueryBuilder boostTermQueryBuilder;
    protected final SearchFieldsAndBoosting boostSearchFieldsAndBoostings;
    protected final boolean addQuerqyBoostQueriesToMainQuery;
    protected String parserDebugInfo = null;
    protected RewriteChainLog rewriteChainLogging = null;

    public QueryParsingController(final LuceneSearchEngineRequestAdapter requestAdapter) {
        this.requestAdapter = requestAdapter;
        this.queryString = getValidatedQueryString();
        needsScores = requestAdapter.needsScores();
        queryAnalyzer = requestAdapter.getQueryAnalyzer();

        final Map<String, Float> queryFieldsAndBoostings = requestAdapter.getQueryFieldsAndBoostings();
        final float gfb = requestAdapter.getGeneratedFieldBoost().orElse(DEFAULT_GENERATED_FIELD_BOOST);
        Map<String, Float> generatedQueryFieldsAndBoostings = requestAdapter.getGeneratedQueryFieldsAndBoostings();
        if (generatedQueryFieldsAndBoostings.isEmpty()) {
            generatedQueryFieldsAndBoostings = queryFieldsAndBoostings
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() * gfb));
        } else {

            for (final Map.Entry<String, Float> entry : generatedQueryFieldsAndBoostings.entrySet()) {
                if (entry.getValue() == null) {
                    final String name = entry.getKey();
                    final Float nonGeneratedBoostFactor = queryFieldsAndBoostings.getOrDefault(name, 1f);
                    entry.setValue(nonGeneratedBoostFactor * gfb);
                }
            }
        }

        // TODO: revisit
        searchFieldsAndBoosting = new SearchFieldsAndBoosting(
                needsScores
                        ? requestAdapter.getFieldBoostModel().orElse(DEFAULT_FIELD_BOOST_MODEL)
                        : FieldBoostModel.FIXED, // TODO: better use NONE as FBM?
                queryFieldsAndBoostings,
                generatedQueryFieldsAndBoostings,
                gfb);

        if (!needsScores) {
            addQuerqyBoostQueriesToMainQuery = true;
            dfc = null;
            boostTermQueryBuilder = null;
            boostSearchFieldsAndBoostings = null;
            builder = new LuceneQueryBuilder(new LuceneTermQueryBuilder(), queryAnalyzer, searchFieldsAndBoosting, 1f,
                    1f, requestAdapter.getTermQueryCache().orElse(null), q -> {
                try {
                    return requestAdapter.rawQueryToQuery(q);
                } catch (final SyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            addQuerqyBoostQueriesToMainQuery = requestAdapter.addQuerqyBoostQueriesToMainQuery();

            final QuerySimilarityScoring userQuerySimilarityScoring = requestAdapter.getUserQuerySimilarityScoring()
                    .orElse(DEFAULT_USER_QUERY_SIMILARITY_SCORING);
            final TermQueryBuilder userTermQueryBuilder = userQuerySimilarityScoring.createTermQueryBuilder(null);
            dfc = userTermQueryBuilder.getDocumentFrequencyCorrection().orElse(null);

            final QuerySimilarityScoring boostQuerySimilarityScoring = requestAdapter.getBoostQuerySimilarityScoring()
                    .orElse(DEFAULT_BOOST_QUERY_SIMILARITY_SCORING);

            boostTermQueryBuilder = boostQuerySimilarityScoring.createTermQueryBuilder(dfc);

            boostSearchFieldsAndBoostings = requestAdapter.useFieldBoostingInQuerqyBoostQueries()
                    ? searchFieldsAndBoosting
                    : searchFieldsAndBoosting.withFieldBoostModel(FieldBoostModel.NONE);




            builder = new LuceneQueryBuilder(userTermQueryBuilder,
                    queryAnalyzer, searchFieldsAndBoosting, requestAdapter.getTiebreaker().orElse(DEFAULT_TIEBREAKER),
                    requestAdapter.getMultiMatchTiebreaker().orElse(DEFAULT_MULTI_MATCH_TIEBREAKER),
                    requestAdapter.getTermQueryCache().orElse(null), q -> {
                try {
                    return requestAdapter.rawQueryToQuery(q);
                } catch (final SyntaxException e) {
                    throw new RuntimeException(e);
                }});

        }
    }

    public ExpandedQuery createExpandedQuery() {
        if (requestAdapter.isMatchAllQuery(queryString)) {
            return new ExpandedQuery(new MatchAllQuery());

        } else {
            final QuerqyParser parser = requestAdapter.createQuerqyParser()
                    .orElseGet(QueryParsingController::newDefaultQuerqyParser);

            // TODO: What is happening here ?!?
            //  build rewrite logging config

            if (requestAdapter.isDebugQuery()) {
                parserDebugInfo = parser.getClass().getName();
            }

            return new ExpandedQuery(parser.parse(queryString));
        }
    }

    public LuceneQueries process() throws SyntaxException {
        final ExpandedQuery parsedInput = createExpandedQuery();

        // additive boosts given e.g. by Solr URL parameters "bq" and "bf"
        final List<Query> additiveBoostsFromRequest = needsScores ? requestAdapter.getAdditiveBoosts(parsedInput.getUserQuery()) : Collections.emptyList();
        final boolean hasAdditiveBoostsFromRequest = !additiveBoostsFromRequest.isEmpty();

        // multiplicative boosts, e.g. by Solr URL parameter "boost"
        final List<Query> multiplicativeBoostsFromRequest = needsScores ? requestAdapter.getMultiplicativeBoosts(parsedInput.getUserQuery()) : Collections.emptyList();
        final boolean hasMultiplicativeBoostsFromRequest = !multiplicativeBoostsFromRequest.isEmpty();

        final RewriteChainOutput rewriteChainOutput = requestAdapter.getRewriteChain().rewrite(parsedInput, requestAdapter);

        if (rewriteChainOutput.getRewriteLog().isPresent()) {
            this.rewriteChainLogging = rewriteChainOutput.getRewriteLog().get();
            processRewriteLogging();
        }

        final ExpandedQuery rewrittenExpandedQuery = rewriteChainOutput.getExpandedQuery();

        Query mainQuery = transformUserQuery(rewrittenExpandedQuery.getUserQuery(), builder);

        if (dfc != null) dfc.finishedUserQuery();


        final List<Query> filterQueries = transformFilterQueries(rewrittenExpandedQuery.getFilterQueries());

        // additive boosts from Querqy query rewriters
        final List<Query> additiveBoostsFromQuerqy = needsScores ? getAdditiveQuerqyBoostQueries(rewrittenExpandedQuery) : Collections.emptyList();
        final boolean hasAdditiveBoostsFromQuerqy = !additiveBoostsFromQuerqy.isEmpty();

        // multiplicative boosts from Querqy query rewriters
        final List<ValueSource> multiplicativeBoostsFromQuerqy = needsScores ? getQuerqyMultiplicativeBoostQueries(rewrittenExpandedQuery) : Collections.emptyList();
        final boolean hasMultiplicativeBoostsFromQuerqy = !multiplicativeBoostsFromQuerqy.isEmpty();

        final boolean hasQuerqyBoostQueriesOnMainQuery = (hasAdditiveBoostsFromQuerqy || hasMultiplicativeBoostsFromQuerqy) && addQuerqyBoostQueriesToMainQuery;

        // do we have to add optional boost query/ies (either from the request or created as part of the rewrite chain)
        // as an optional clause to the main query or wrap the main query's scoring into a multiplicative function?
        final boolean mainQueryNeedsBoost = needsScores &&
                (hasAdditiveBoostsFromRequest || hasMultiplicativeBoostsFromRequest || hasQuerqyBoostQueriesOnMainQuery);

        final Query userQuery = mainQuery;

        if (mainQueryNeedsBoost) {

            final BooleanQuery.Builder builder = new BooleanQuery.Builder();

            if (mainQuery instanceof MatchAllDocsQuery) {
                builder.add(mainQuery, BooleanClause.Occur.FILTER);
            } else {
                builder.add(LuceneQueryUtil.boost(mainQuery, requestAdapter.getUserQueryWeight().orElse(1f)),
                        BooleanClause.Occur.MUST);
            }

            for (final Query f : additiveBoostsFromRequest) {
                builder.add(f, BooleanClause.Occur.SHOULD);
            }

            if (hasQuerqyBoostQueriesOnMainQuery) {
                for (final Query q : additiveBoostsFromQuerqy) {
                    builder.add(q, BooleanClause.Occur.SHOULD);
                }
            }

            final BooleanQuery bq = builder.build();

            if (hasMultiplicativeBoostsFromRequest || hasMultiplicativeBoostsFromQuerqy) {
                ValueSource[] multiplicativeValueSources = Stream.concat(
                        multiplicativeBoostsFromRequest.stream().map(LuceneQueryUtil::queryToValueSource),
                        multiplicativeBoostsFromQuerqy.stream()
                ).toArray(ValueSource[]::new);

                if (multiplicativeValueSources.length > 1) {
                    mainQuery = FunctionScoreQuery.boostByValue(bq, new ProductFloatFunction(multiplicativeValueSources).asDoubleValuesSource());
                } else {
                    mainQuery = FunctionScoreQuery.boostByValue(bq, multiplicativeValueSources[0].asDoubleValuesSource());
                }
            } else {
                mainQuery = bq;
            }
        }

        LuceneQueries luceneQueries;
        if ((!addQuerqyBoostQueriesToMainQuery) && hasAdditiveBoostsFromQuerqy) {
            // boost queries have not been applied to the main query, they are returned separately to be applied as QuerqyReRankQueries
            // externally requested re-rank queries (via querqy.rq) are ignored
            //
            // todo: this currently ignores Querqy multiplicativeBoosts as the QuerqyReRankQuery performs an addition of optionally matching SHOULD clause scores
            luceneQueries = new LuceneQueries(mainQuery, filterQueries, additiveBoostsFromQuerqy, userQuery, null, dfc != null,
                    false);
        } else {
            Query rankQuery = requestAdapter.parseRankQuery().orElse(null);
            luceneQueries = new LuceneQueries(mainQuery, filterQueries, null, userQuery, rankQuery, dfc != null,
                    hasQuerqyBoostQueriesOnMainQuery);
        }
        return luceneQueries;
    }

    public List<Query> transformFilterQueries(final Collection<QuerqyQuery<?>> filterQueries) throws SyntaxException {

        if (filterQueries != null && !filterQueries.isEmpty()) {

            final List<Query> fqs = new LinkedList<>();

            for (final QuerqyQuery<?> qfq : filterQueries) {

                if (qfq instanceof RawQuery) {

                    fqs.add(requestAdapter.rawQueryToQuery((RawQuery) qfq));

                } else {

                    builder.reset();

                    fqs.add(builder.createQuery(qfq));

                }
            }

            return fqs;
        } else {
            return Collections.emptyList();
        }

    }


    protected String getValidatedQueryString() {
        final String queryString = requestAdapter.getQueryString();
        if (queryString == null) {
            throw new IllegalArgumentException("Query string must not be null");
        }

        final String qs = queryString.trim();
        if (qs.isEmpty()) {
            throw new IllegalArgumentException("Query string must not be empty");
        }
        return qs;
    }


    public Query transformUserQuery(final QuerqyQuery<?> querqyUserQuery, final LuceneQueryBuilder builder) {

        builder.reset();

        final Query query = builder.createQuery(querqyUserQuery);
        final Query userQuery = (query instanceof BooleanQuery)
                ? requestAdapter.applyMinimumShouldMatch((BooleanQuery) query)
                : query;

        return needsScores || (userQuery instanceof MatchAllDocsQuery) ? userQuery : new ConstantScoreQuery(userQuery);

    }

    protected List<Query> getAdditiveQuerqyBoostQueries(final ExpandedQuery expandedQuery) throws SyntaxException {

        final List<Query> result = transformAdditiveBoostQueries(expandedQuery.getBoostUpQueries(),
                requestAdapter.getPositiveQuerqyBoostWeight().orElse(DEFAULT_POSITIVE_QUERQY_BOOST_WEIGHT));
        final List<Query> down = transformAdditiveBoostQueries(expandedQuery.getBoostDownQueries(),
                -requestAdapter.getNegativeQuerqyBoostWeight().map(Math::abs).orElse(DEFAULT_NEGATIVE_QUERQY_BOOST_WEIGHT));

        if (down != null) {
            if (result == null) {
                return down;
            } else {
                result.addAll(down);
            }
        }

        return result != null ? result : Collections.emptyList();

    }


    public List<Query> transformAdditiveBoostQueries(final Collection<BoostQuery> boostQueries, final float factor)
            throws SyntaxException {

        final List<Query> result;

        if (boostQueries != null && !boostQueries.isEmpty()) {

            result = new LinkedList<>();

            for (final BoostQuery bq : boostQueries) {

                final Query luceneQuery;
                final QuerqyQuery<?> boostQuery = bq.getQuery();

                if (boostQuery instanceof RawQuery) {

                    luceneQuery = requestAdapter.rawQueryToQuery((RawQuery) boostQuery);

                } else if (boostQuery instanceof querqy.model.Query) {

                    final LuceneQueryBuilder luceneQueryBuilder =
                            new LuceneQueryBuilder(boostTermQueryBuilder, queryAnalyzer,
                                    boostSearchFieldsAndBoostings,
                                    requestAdapter.getTiebreaker().orElse(DEFAULT_TIEBREAKER),
                                    1f, // we don't have to apply multiMatchTie for boostings
                                    requestAdapter.getTermQueryCache().orElse(null), q -> {
                                try {
                                    return requestAdapter.rawQueryToQuery(q);
                                } catch (final SyntaxException e) {
                                    throw new RuntimeException(e);
                                }});

                    luceneQuery = luceneQueryBuilder.createQuery((querqy.model.Query) boostQuery, factor < 0f);

                } else {
                    luceneQuery = null;
                }

                if (luceneQuery != null) {

                    final Query queryToAdd;
                    final float boost;

                    if (luceneQuery instanceof BooleanQuery) {

                        final BooleanQuery booleanQuery = ((BooleanQuery) luceneQuery);
                        final List<BooleanClause> clauses = booleanQuery.clauses();

                        final List<BooleanClause> mustNotClauses = clauses.stream()
                                .filter(clause -> clause.occur() == BooleanClause.Occur.MUST_NOT)
                                .collect(Collectors.toList());

                        if (mustNotClauses.size() == clauses.size()) {

                            // boosting on purely negative query, apply negated boost on the negated query
                            final BooleanQuery.Builder builder = new BooleanQuery.Builder();
                            builder.setMinimumNumberShouldMatch(booleanQuery.getMinimumNumberShouldMatch());
                            mustNotClauses.forEach(q -> builder.add(q.query(), BooleanClause.Occur.MUST));

                            queryToAdd = builder.build();

                            boost = -bq.getBoost() * factor;
                            if (boost != 1f) {

                                final QueryValueSource queryValueSource = new QueryValueSource(luceneQuery, 0f);
                                result.add(new FunctionQuery(new AdditiveBoostFunction(queryValueSource, boost)));

                            } else {
                                result.add(luceneQuery);

                            }
                        } else {
                            queryToAdd = luceneQuery;
                            boost = bq.getBoost() * factor;
                        }

                    } else {
                        queryToAdd = luceneQuery;
                        boost = bq.getBoost() * factor;
                    }

                    if (boost != 1f) {

                        final QueryValueSource queryValueSource = new QueryValueSource(queryToAdd, 0f);
                        result.add(new FunctionQuery(new AdditiveBoostFunction(queryValueSource, boost)));

                    } else {
                        result.add(queryToAdd);

                    }

                }

            }

        } else {
            result = null;
        }

        return result;
    }

    protected List<ValueSource> getQuerqyMultiplicativeBoostQueries(ExpandedQuery expandedQuery) throws SyntaxException {
        final List<ValueSource> result = transformMultiplicativeBoostQueries(expandedQuery.getMultiplicativeBoostQueries());
        return result != null ? result : Collections.emptyList();
    }

    protected List<ValueSource> transformMultiplicativeBoostQueries(Collection<BoostQuery> boostQueries) throws SyntaxException {
        final List<ValueSource> result;

        if (boostQueries != null && !boostQueries.isEmpty()) {
            result = new LinkedList<>();

            for (final BoostQuery boostQuery : boostQueries) {
                final Query luceneQuery;
                final QuerqyQuery<?> query = boostQuery.getQuery();

                // todo: this is copied from transformAdditiveBoostQueries, any way to combine or simplify?
                if (query instanceof RawQuery) {
                    luceneQuery = requestAdapter.rawQueryToQuery((RawQuery) query);
                } else if (query instanceof querqy.model.Query) {
                    final LuceneQueryBuilder luceneQueryBuilder =
                            new LuceneQueryBuilder(boostTermQueryBuilder, queryAnalyzer,
                                    boostSearchFieldsAndBoostings,
                                    requestAdapter.getTiebreaker().orElse(DEFAULT_TIEBREAKER),
                                    1f, // we don't have to apply multiMatchTie for boostings
                                    requestAdapter.getTermQueryCache().orElse(null), q -> {
                                try {
                                    return requestAdapter.rawQueryToQuery(q);
                                } catch (final SyntaxException e) {
                                    throw new RuntimeException(e);
                                }});
                    luceneQuery = luceneQueryBuilder.createQuery((querqy.model.Query) query, true);
                } else {
                    luceneQuery = null;
                }

                if (luceneQuery != null) {
                    ValueSource queryValueSource = new QueryValueSource(luceneQuery, 0f);
                    ValueSource matchingValue = new ConstValueSource(boostQuery.getBoost());
                    ValueSource nonMatchingValue = new ConstValueSource(1f);

                    // create a BoolFunction as an input for multiplication that emits the boost factor if the query matches, or 1f if not
                    IfFunction boostIf = new IfFunction(queryValueSource, matchingValue, nonMatchingValue);
                    result.add(boostIf);
                }
            }
        } else {
            result = null;
        }

        return result;
    }

    private void processRewriteLogging() {
        requestAdapter.getInfoLoggingContext().ifPresent(
                infoLoggingContext -> rewriteChainLogging.getRewriteChain().forEach(
                        rewriteLoggingEntry -> {
                            infoLoggingContext.setRewriterId(rewriteLoggingEntry.getRewriterId());
                            final List<Object> rewriteActions = REWRITE_LOGGING_OBJECT_MAPPER.convertValue(
                                    rewriteLoggingEntry.getActions(), new TypeReference<>() {});
                            infoLoggingContext.log(rewriteActions);
                        }
                )
        );
    }

    public Map<String, Object> getDebugInfo() {

        if (requestAdapter.isDebugQuery()) {

            Map<String, Object> info = new TreeMap<>();

            if (parserDebugInfo != null) {
                info.put("parser", parserDebugInfo);
            }

            if (rewriteChainLogging != null && !rewriteChainLogging.getRewriteChain().isEmpty()) {
                info.put(
                        "rewrite",
                        REWRITE_LOGGING_OBJECT_MAPPER.convertValue(rewriteChainLogging, new TypeReference<>() {})
                );
            }
            return info;

        } else {

            return Collections.emptyMap();

        }
    }

    protected static QuerqyParser newDefaultQuerqyParser() {
        try {
            return DEFAULT_PARSER_CLASS.newInstance();
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


}
