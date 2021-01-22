package querqy.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queries.function.FunctionQuery;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.queries.function.valuesource.ProductFloatFunction;
import org.apache.lucene.queries.function.valuesource.QueryValueSource;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.queries.function.ValueSource;

import querqy.lucene.LuceneSearchEngineRequestAdapter.SyntaxException;
import querqy.lucene.rewrite.DocumentFrequencyCorrection;
import querqy.lucene.rewrite.LuceneQueryBuilder;
import querqy.lucene.rewrite.LuceneTermQueryBuilder;
import querqy.lucene.rewrite.AdditiveBoostFunction;
import querqy.lucene.rewrite.SearchFieldsAndBoosting;
import querqy.lucene.rewrite.SearchFieldsAndBoosting.FieldBoostModel;
import querqy.lucene.rewrite.TermQueryBuilder;
import querqy.model.BoostQuery;
import querqy.model.ExpandedQuery;
import querqy.model.MatchAllQuery;
import querqy.model.QuerqyQuery;
import querqy.model.RawQuery;
import querqy.parser.QuerqyParser;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.ContextAwareQueryRewriter;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

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


    protected final LuceneSearchEngineRequestAdapter requestAdapter;
    protected final String queryString;
    protected final boolean needsScores;
    protected final Analyzer queryAnalyzer;
    protected final SearchFieldsAndBoosting searchFieldsAndBoosting;
    protected final DocumentFrequencyCorrection dfc;
    protected final boolean debugQuery;
    protected final LuceneQueryBuilder builder;
    protected final TermQueryBuilder boostTermQueryBuilder;
    protected final SearchFieldsAndBoosting boostSearchFieldsAndBoostings;
    protected final boolean addQuerqyBoostQueriesToMainQuery;
    protected String parserDebugInfo = null;

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
                        : FieldBoostModel.FIXED,
                queryFieldsAndBoostings,
                generatedQueryFieldsAndBoostings,
                gfb);

        if (!needsScores) {
            addQuerqyBoostQueriesToMainQuery = true;
            dfc = null;
            boostTermQueryBuilder = null;
            boostSearchFieldsAndBoostings = null;
            builder = new LuceneQueryBuilder(new LuceneTermQueryBuilder(), queryAnalyzer, searchFieldsAndBoosting, 1f,
                    requestAdapter.getTermQueryCache().orElse(null));
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
                    requestAdapter.getTermQueryCache().orElse(null));

        }


        debugQuery = requestAdapter.isDebugQuery();


    }

    public ExpandedQuery createExpandedQuery() {
        if (requestAdapter.isMatchAllQuery(queryString)) {
            return new ExpandedQuery(new MatchAllQuery());

        } else {
            final QuerqyParser parser = requestAdapter.createQuerqyParser()
                    .orElseGet(QueryParsingController::newDefaultQuerqyParser);
            if (debugQuery) {
                parserDebugInfo = parser.getClass().getName();
            }

            return new ExpandedQuery(parser.parse(queryString));
        }
    }

    public LuceneQueries process() throws SyntaxException {
        final ExpandedQuery parsedInput = createExpandedQuery();

        final List<Query> additiveBoosts;
        final List<Query> multiplicativeBoosts;


        if (needsScores) {
            additiveBoosts = requestAdapter.getAdditiveBoosts(parsedInput.getUserQuery());
            multiplicativeBoosts = requestAdapter.getMultiplicativeBoosts(parsedInput.getUserQuery());
        } else {
            additiveBoosts = multiplicativeBoosts = null;
        }


        final Map<String, Object> context = requestAdapter.getContext();
        if (debugQuery) {
            context.put(ContextAwareQueryRewriter.CONTEXT_KEY_DEBUG_ENABLED, true);
        }

        final ExpandedQuery rewrittenQuery = requestAdapter.getRewriteChain().rewrite(parsedInput, requestAdapter);

        Query mainQuery = transformUserQuery(rewrittenQuery.getUserQuery(), builder);

        if (dfc != null) dfc.finishedUserQuery();


        final List<Query> filterQueries = transformFilterQueries(rewrittenQuery.getFilterQueries());
        final List<Query> querqyBoostQueries = needsScores
                ? getQuerqyBoostQueries(rewrittenQuery)
                : Collections.emptyList();

        final Query userQuery = mainQuery;

        final boolean hasMultiplicativeBoosts = multiplicativeBoosts != null && !multiplicativeBoosts.isEmpty();
        final boolean hasQuerqyBoostQueries = !querqyBoostQueries.isEmpty();
        final boolean hasQuerqyBoostQueriesOnMainQuery = hasQuerqyBoostQueries && addQuerqyBoostQueriesToMainQuery;

        // do we have to add a boost query as an optional clause to the main query?
        final boolean hasOptBoost = needsScores &&
                ((additiveBoosts != null && !additiveBoosts.isEmpty())
                        || hasMultiplicativeBoosts
                        || hasQuerqyBoostQueriesOnMainQuery);

        if (hasOptBoost) {

            final BooleanQuery.Builder builder = new BooleanQuery.Builder();

            if (mainQuery instanceof MatchAllDocsQuery) {
                builder.add(mainQuery, BooleanClause.Occur.FILTER);
            } else {
                builder.add(LuceneQueryUtil.boost(mainQuery, requestAdapter.getUserQueryWeight().orElse(1f)),
                        BooleanClause.Occur.MUST);
            }

            if (additiveBoosts != null) {
                for (final Query f : additiveBoosts) {
                    builder.add(f, BooleanClause.Occur.SHOULD);
                }
            }

            if (hasQuerqyBoostQueriesOnMainQuery) {
                for (final Query q : querqyBoostQueries) {
                    builder.add(q, BooleanClause.Occur.SHOULD);
                }
            }

            final BooleanQuery bq = builder.build();

            if (hasMultiplicativeBoosts) {

                if (multiplicativeBoosts.size() > 1) {
                    final ValueSource prod = new ProductFloatFunction(
                            multiplicativeBoosts
                                .stream()
                                .map(LuceneQueryUtil::queryToValueSource)
                                .toArray(ValueSource[]::new)
                    );
                    mainQuery = FunctionScoreQuery.boostByValue(bq, prod.asDoubleValuesSource());
                } else {
                    mainQuery = FunctionScoreQuery.boostByValue(bq, LuceneQueryUtil.queryToDoubleValueSource(multiplicativeBoosts.get(0)));
                }
            } else {
                mainQuery = bq;
            }
        }

        LuceneQueries luceneQueries;
        if ((!addQuerqyBoostQueriesToMainQuery) && hasQuerqyBoostQueries) {
            // boost queries have not been applied to the main query, they are returned separately here, external rank queries are ignored
            luceneQueries = new LuceneQueries(mainQuery, filterQueries, querqyBoostQueries, userQuery, null, dfc != null,
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

                    fqs.add(requestAdapter.parseRawQuery((RawQuery) qfq));

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

    protected List<Query> getQuerqyBoostQueries(final ExpandedQuery expandedQuery) throws SyntaxException {

        final List<Query> result = transformBoostQueries(expandedQuery.getBoostUpQueries(),
                requestAdapter.getPositiveQuerqyBoostWeight().orElse(DEFAULT_POSITIVE_QUERQY_BOOST_WEIGHT));
        final List<Query> down = transformBoostQueries(expandedQuery.getBoostDownQueries(),
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


    public List<Query> transformBoostQueries(final Collection<BoostQuery> boostQueries, final float factor)
            throws SyntaxException {

        final List<Query> result;

        if (boostQueries != null && !boostQueries.isEmpty()) {

            result = new LinkedList<>();

            for (final BoostQuery bq : boostQueries) {

                final Query luceneQuery;
                final QuerqyQuery<?> boostQuery = bq.getQuery();

                if (boostQuery instanceof RawQuery) {

                    luceneQuery = requestAdapter.parseRawQuery((RawQuery) boostQuery);

                } else if (boostQuery instanceof querqy.model.Query) {

                    final LuceneQueryBuilder luceneQueryBuilder =
                            new LuceneQueryBuilder(boostTermQueryBuilder, queryAnalyzer,
                                    boostSearchFieldsAndBoostings,
                                    requestAdapter.getTiebreaker().orElse(DEFAULT_TIEBREAKER),
                                    requestAdapter.getTermQueryCache().orElse(null));

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
                                .filter(clause -> clause.getOccur() == BooleanClause.Occur.MUST_NOT)
                                .collect(Collectors.toList());

                        if (mustNotClauses.size() == clauses.size()) {

                            // boosting on purely negative query, apply negated boost on the negated query
                            final BooleanQuery.Builder builder = new BooleanQuery.Builder();
                            builder.setMinimumNumberShouldMatch(booleanQuery.getMinimumNumberShouldMatch());
                            mustNotClauses.forEach(q -> builder.add(q.getQuery(), BooleanClause.Occur.MUST));

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

    public Map<String, Object> getDebugInfo() {

        if (debugQuery) {

            Map<String, Object> info = new TreeMap<>();

            if (parserDebugInfo != null) {
                info.put("querqy.parser", parserDebugInfo);
            }
            final Object contextDebugInfo = requestAdapter.getContext()
                    .get(ContextAwareQueryRewriter.CONTEXT_KEY_DEBUG_DATA);
            if (contextDebugInfo != null) {
                info.put("querqy.rewrite", contextDebugInfo);
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
