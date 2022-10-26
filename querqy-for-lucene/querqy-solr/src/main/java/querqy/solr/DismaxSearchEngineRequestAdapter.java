package querqy.solr;

import static querqy.lucene.PhraseBoosting.makePhraseFieldsBoostQuery;
import static querqy.solr.QuerqyDismaxParams.*;
import static querqy.solr.RewriteLoggingParameter.REWRITE_LOGGING_PARAM_KEY;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.AppendedSolrParams;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.TextField;
import org.apache.solr.search.DisMaxQParser;
import org.apache.solr.search.FieldParams;
import org.apache.solr.search.FunctionQParserPlugin;
import org.apache.solr.search.QParser;
import org.apache.solr.search.RankQuery;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.util.SolrPluginUtils;
import querqy.lucene.PhraseBoosting;
import querqy.lucene.PhraseBoosting.PhraseBoostFieldParams;
import querqy.lucene.QuerySimilarityScoring;
import querqy.lucene.LuceneSearchEngineRequestAdapter;
import querqy.lucene.rewrite.SearchFieldsAndBoosting;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.lucene.rewrite.infologging.InfoLogging;
import querqy.lucene.rewrite.infologging.InfoLoggingContext;
import querqy.model.ParametrizedRawQuery;
import querqy.model.QuerqyQuery;
import querqy.model.RawQuery;
import querqy.model.StringRawQuery;
import querqy.model.logging.RewriteLoggingConfig;
import querqy.parser.QuerqyParser;
import querqy.rewrite.RewriteChain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>A {@link LuceneSearchEngineRequestAdapter} that provides access from Querqy to Solr and that implements most of
 * Solr's {@link org.apache.solr.search.ExtendedDismaxQParser} behaviour. See constants of this class for default
 * behaviour that is not defined in {@link LuceneSearchEngineRequestAdapter}. See {@link QuerqyDismaxParams} for parameter
 * names.</p>
 * @see QuerqyDismaxParams
 * @see LuceneSearchEngineRequestAdapter
 */
public class DismaxSearchEngineRequestAdapter implements LuceneSearchEngineRequestAdapter {


    /**
     * The default value for {@link QuerqyDismaxParams#QBOOST_RERANK_NUMDOCS}
     */
    public static final int DEFAULT_RERANK_NUMDOCS      = 500;

    /**
     * The default boost method (= {@link QuerqyDismaxParams#QBOOST_METHOD})
     */
    public static final String QBOOST_METHOD_DEFAULT = QBOOST_METHOD_OPT;


    public static final String DEFAULT_QBOOST_FIELD_BOOST = QBOOST_FIELD_BOOST_ON;


    protected static final float DEFAULT_QPF_TIE = 0f;


    protected static final String MATCH_ALL_QUERY_STRING = "*:*";

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final Pattern PATTERN_WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern PATTERN_CARAT = Pattern.compile("\\^");

    private final String userQueryString;
    private final SolrParams solrParams;
    private final SolrQueryRequest request;
    private final TermQueryCache termQueryCache;
    private final QuerqyParser querqyParser;
    private final RewriteChain rewriteChain;
    private final List<FieldParams> allPhraseFields;
    private final String minShouldMatch;
    private final Map<String, Object> context;
    private final QParser qParser;
    private final InfoLoggingContext infoLoggingContext;

    private final boolean isDebug;

    private final RewriteLoggingParameter rewriteLoggingParameter;
    private final RewriteLoggingConfig rewriteLoggingConfig;

    private Map<String, String> additionalParams = null;

    public DismaxSearchEngineRequestAdapter(final QParser qParser, final SolrQueryRequest request,
                                            final String queryString, final SolrParams solrParams,
                                            final QuerqyParser querqyParser, final RewriteChain rewriteChain,
                                            final InfoLogging infoLogging,
                                            final TermQueryCache termQueryCache) {
        this.qParser = qParser;
        this.userQueryString = queryString;
        this.solrParams = solrParams;
        this.termQueryCache = termQueryCache;

        this.querqyParser = querqyParser;
        this.request = request;
        this.rewriteChain = rewriteChain;
        this.context = new HashMap<>();
        this.isDebug = solrParams.getBool(CommonParams.DEBUG_QUERY, false);

        this.rewriteLoggingParameter = RewriteLoggingParameter.of(solrParams.get(REWRITE_LOGGING_PARAM_KEY, "OFF"));
        this.rewriteLoggingConfig = createRewriteLoggingConfig();
        if (infoLogging != null && rewriteLoggingConfig.isActive()) {
            this.infoLoggingContext = new InfoLoggingContext(infoLogging, this);

        } else {
            this.infoLoggingContext = null;
        }

        final int ps0 = solrParams.getInt(PS, 0);
        final int ps2 = solrParams.getInt(PS2, ps0);
        final int ps3 = solrParams.getInt(PS3, ps0);


        final List<FieldParams> phraseFields = SolrPluginUtils
                .parseFieldBoostsAndSlop(solrParams.getParams(PF),0,ps0);
        final List<FieldParams> phraseFields2 = SolrPluginUtils
                .parseFieldBoostsAndSlop(solrParams.getParams(PF2),2,ps2);
        final List<FieldParams> phraseFields3 = SolrPluginUtils
                .parseFieldBoostsAndSlop(solrParams.getParams(PF3),3,ps3);

        allPhraseFields = new ArrayList<>(phraseFields.size() + phraseFields2.size() + phraseFields3.size());
        allPhraseFields.addAll(phraseFields);
        allPhraseFields.addAll(phraseFields2);
        allPhraseFields.addAll(phraseFields3);

        minShouldMatch = DisMaxQParser.parseMinShouldMatch(request.getSchema(), solrParams);

    }

    private RewriteLoggingConfig createRewriteLoggingConfig() {
        if (isDebug || RewriteLoggingParameter.DETAILS.equals(rewriteLoggingParameter)) {
            return RewriteLoggingConfig.details();

        } else if (RewriteLoggingParameter.REWRITER_ID.equals(rewriteLoggingParameter)) {
            return RewriteLoggingConfig.idsOnly();

        } else {
            return RewriteLoggingConfig.off();
        }
    }

    @Override
    public String getQueryString() {
        return userQueryString;
    }

    @Override
    public boolean isMatchAllQuery(final String queryString) {

        return (queryString.charAt(0) == '*')
                && (queryString.length() == 1 || MATCH_ALL_QUERY_STRING.equals(queryString));

    }

    @Override
    public boolean needsScores() {
        return solrParams.getBool(NEEDS_SCORES, true);
    }

    @Override
    public Optional<TermQueryCache> getTermQueryCache() {
        return Optional.ofNullable(termQueryCache);
    }

    @Override
    public boolean addQuerqyBoostQueriesToMainQuery() {
        return QBOOST_METHOD_OPT.equals(solrParams.get(QBOOST_METHOD, QBOOST_METHOD_DEFAULT));
    }

    public int getReRankNumDocs() {
        return solrParams.getInt(QBOOST_RERANK_NUMDOCS, DEFAULT_RERANK_NUMDOCS);
    }

    @Override
    public Optional<QuerySimilarityScoring> getUserQuerySimilarityScoring() {
        return getSimilarityScoringParam(USER_QUERY_SIMILARITY_SCORE);
    }

    @Override
    public Optional<Float> getUserQueryWeight() {
        return getFloatRequestParam(USER_QUERY_BOOST);
    }

    @Override
    public Optional<QuerySimilarityScoring> getBoostQuerySimilarityScoring() {
        return getSimilarityScoringParam(QBOOST_SIMILARITY_SCORE);
    }

    public boolean useFieldBoostingInQuerqyBoostQueries() {

        final String boostFieldBoost = solrParams.get(QBOOST_FIELD_BOOST, DEFAULT_QBOOST_FIELD_BOOST);
        switch (boostFieldBoost) {
            case QBOOST_FIELD_BOOST_ON:
                return true;
            case QBOOST_FIELD_BOOST_OFF:
                return false;
            default:
                throw new IllegalArgumentException("Invalid value for " + QBOOST_FIELD_BOOST + ": "
                        + boostFieldBoost);
        }
    }

    @Override
    public Optional<Float> getTiebreaker() {
        return getFloatRequestParam(TIE);
    }

    @Override
    public Optional<Float> getMultiMatchTiebreaker() {
        return getFloatRequestParam(MULTI_MATCH_TIE);
    }

    @Override
    public boolean isDebugQuery() {
        return isDebug;
    }

    @Override
    public Optional<QuerqyParser> createQuerqyParser() {
        return Optional.ofNullable(querqyParser);
    }

    /**
     * <p>Get the list of {@link PhraseBoosting.PhraseBoostFieldParams} to boost entire phrases or query-term n-grams.</p>
     * @see #getPhraseBoostTiebreaker()
     *
     * @return The list of PhraseBoostFieldParams for boosting or an empty list if no (sub)phrase should be boosted.
     */
    public List<PhraseBoostFieldParams> getPhraseBoostFieldParams() {

        final IndexSchema schema = request.getSchema();

        return allPhraseFields.stream()
                .filter(field -> isFieldPhraseQueryable(schema.getFieldOrNull(field.getField())))
                .map(DismaxSearchEngineRequestAdapter::fieldParams2phraseBoostFieldParams)
                .collect(Collectors.toList());

    }

    /**
     * <p>Get an optional tiebreaker for combining phrase boosts.</p>
     *
     * <p>When query (sub)phrase boosting is enabled via {@link #getPhraseBoostFieldParams()}, phrases could be boosted
     * several times if boosting is enabled for query term bi-grams, tri-grams and the entire phrase because the bi-grams
     * would be contained in the tri-grams and the tri-grams would be contained in the exact query phrase.
     * The {@link PhraseBoosting#makePhraseFieldsBoostQuery(QuerqyQuery, List, float, Analyzer)} combines
     * the boost queries for the different n-gram/phrase levels using
     * a {@link org.apache.lucene.search.DisjunctionMaxQuery}. This tiebreaker controls how much weight is given to the
     * n-gram/phrase level with the highest score vs the other n-gram/phrase levels. A tiebreaker value of
     * 0.0 would disable all phrase boostings except for the highest scoring n-gram/phrase level. A value of 1.0 would
     * sum up the phrase boosts of all levels, which would be the behaviour of the pf, pf2, pf3 params in Solr's
     * edismax query parser.</p>
     *
     * @see #getPhraseBoostFieldParams()
     * @return The value of request parameter {@value QuerqyDismaxParams#QPF_TIE}, defaults to {@value #DEFAULT_QPF_TIE}
     */
    public float getPhraseBoostTiebreaker() {
        return solrParams.getFloat(QPF_TIE, DEFAULT_QPF_TIE);
    }

    @Override
    public RewriteChain getRewriteChain() {
        return rewriteChain;
    }

    @Override
    public Optional<Float> getPositiveQuerqyBoostWeight() {
        return getFloatRequestParam(QBOOST_WEIGHT);
    }

    @Override
    public Optional<Float> getNegativeQuerqyBoostWeight() {
        return getFloatRequestParam(QBOOST_NEG_WEIGHT);
    }

    @Override
    public Map<String, Float> getQueryFieldsAndBoostings() {
        return parseQueryFields(QF, 1f, true);
    }

    @Override
    public Map<String, Object> getContext() {
        return context;
    }

    @Override
    public Optional<InfoLoggingContext> getInfoLoggingContext() {
        return Optional.ofNullable(infoLoggingContext);
    }

    @Override
    public Analyzer getQueryAnalyzer() {
        return request.getSchema().getQueryAnalyzer();
    }

    @Override
    public Query parseRawQuery(final RawQuery rawQuery) throws SyntaxException {
        try {
            if (rawQuery instanceof StringRawQuery) {
                return QParser.getParser(((StringRawQuery) rawQuery).getQueryString(),
                        null, request).getQuery();

            } else if (rawQuery instanceof ParametrizedRawQuery) {
                if (this.additionalParams == null) {
                    this.additionalParams = new HashMap<>();
                    request.setParams(AppendedSolrParams.wrapAppended(request.getParams(),
                            new MapSolrParams(this.additionalParams)));
                }

                final String queryString = ((ParametrizedRawQuery) rawQuery).buildQueryString(
                        param -> {
                            final String paramReference = "querqy.internal.param." + this.additionalParams.size();
                            this.additionalParams.put(paramReference, param);
                            return "$" + paramReference;
                        });

                return QParser.getParser(queryString,null, request).getQuery();


            } else {
                throw new UnsupportedOperationException("Implementation type of RawQuery is not supported for this adapter");
            }

        } catch (SyntaxError syntaxError) {
            throw new SyntaxException(syntaxError);
        }
    }

    @Override
    public Query applyMinimumShouldMatch(final BooleanQuery query) {

        final List<BooleanClause> clauses = query.clauses();
        if (clauses.size() < 2) {
            return query;
        }

        for (final BooleanClause clause : clauses) {
            if ((clause.getQuery() instanceof BooleanQuery) && (clause.getOccur() != BooleanClause.Occur.MUST)) {
                return query; // seems to be a complex query with sub queries - do not
                // apply mm
            }
        }

        return SolrPluginUtils.setMinShouldMatch(query, minShouldMatch);

    }



    @Override
    public Map<String, Float> getGeneratedQueryFieldsAndBoostings()  {
        return parseQueryFields(GQF, null, false);
    }

    @Override
    public Optional<Float> getGeneratedFieldBoost() {
        return getFloatRequestParam(GFB);
    }

    @Override
    public Optional<SearchFieldsAndBoosting.FieldBoostModel> getFieldBoostModel() {

        final String fbm = solrParams.get(FBM);
        if (fbm == null) {
            return Optional.empty();
        }
        switch (fbm) {
            case FBM_FIXED: return Optional.of(SearchFieldsAndBoosting.FieldBoostModel.FIXED);
            case FBM_PRMS: return Optional.of(SearchFieldsAndBoosting.FieldBoostModel.PRMS);
            default: throw new IllegalArgumentException("Unknown field boost model: " + fbm);
        }

    }

    @Override
    public List<Query> getAdditiveBoosts(final QuerqyQuery<?> userQuery) throws SyntaxException {

        final List<Query> boostQueries = parseQueriesFromParam(BQ, null);

        final List<PhraseBoostFieldParams> phraseBoostFieldParams = getPhraseBoostFieldParams();
        final Optional<Query> phraseBoostQuery =
                (!phraseBoostFieldParams.isEmpty())
                        ? makePhraseFieldsBoostQuery(userQuery, phraseBoostFieldParams, getPhraseBoostTiebreaker(),
                            getQueryAnalyzer())
                        : Optional.empty();


        final String[] bfs = getRequestParams(BF);

        final List<Query> boosts = new ArrayList<>(boostQueries.size()
                + bfs.length
                + (phraseBoostQuery.isPresent() ? 1 : 0));

        boosts.addAll(boostQueries);
        phraseBoostQuery.ifPresent(boosts::add);

        for (final String bf : bfs) {

            if (bf != null && bf.trim().length() > 0) {

                final Map<String, Float> ff = SolrPluginUtils.parseFieldBoosts(bf);
                for (final Map.Entry<String, Float> bfAndBoost : ff.entrySet()) {

                    try {
                        final Query fq = qParser.subQuery(bfAndBoost.getKey(), FunctionQParserPlugin.NAME).getQuery();
                        final Float b = bfAndBoost.getValue();
                        if (null != b && b != 1f) {
                            boosts.add(new BoostQuery(fq, b));
                        } else {
                            boosts.add(fq);
                        }
                    } catch (final SyntaxError syntaxError) {
                        throw new SyntaxException(syntaxError);
                    }

                }
            }
        }

        return boosts;
    }

    @Override
    public List<Query> getMultiplicativeBoosts(final QuerqyQuery<?> userQuery) throws SyntaxException {
        return parseQueriesFromParam(MULT_BOOST, FunctionQParserPlugin.NAME);
    }

    @Override
    public Optional<Query> parseRankQuery() throws SyntaxException {
        Optional<String> rankQueryStringOpt = getRequestParam(QRQ);
        if (rankQueryStringOpt.isPresent()) {
            // see org.apache.solr.handler.component.QueryComponent#prepare
            try {
                Query rq = QParser.getParser(rankQueryStringOpt.get(), request).getQuery();
                if (rq instanceof RankQuery) {
                    return Optional.of(rq);
                } else {
                    throw new IllegalArgumentException(QRQ + " must be resolved to RankQuery.");
                }
            } catch (SyntaxError e) {
                throw new SyntaxException(e);
            }
        } else {
            return Optional.empty();
        }
    }

    private List<Query> parseQueriesFromParam(final String paramName, final String defaultParserName) throws SyntaxException {

        final String[] qStrings = getRequestParams(paramName);
        if (qStrings.length > 0) {
            final List<Query> result = new ArrayList<>(qStrings.length);
            for (String qs : qStrings) {
                if (qs != null && qs.trim().length()> 0) {
                    try {
                        result.add(qParser.subQuery(qs, defaultParserName).getQuery());
                    } catch (final SyntaxError syntaxError) {
                        throw new SyntaxException(syntaxError);
                    }
                }
            }
            return result;
        }

        return Collections.emptyList();

    }

    // copied from org.apache.solr.util.SolrPluginUtils, allowing for a default boost
    public static Map<String, Float> parseFieldBoosts(final String[] fieldLists, final Float defaultBoost) {
        if (null == fieldLists || 0 == fieldLists.length) {
            return new HashMap<>();
        }
        final Map<String, Float> out = new HashMap<>(7);
        for (String in : fieldLists) {
            if (null == in) {
                continue;
            }
            in = in.trim();
            if (in.length() == 0) {
                continue;
            }

            final String[] bb = PATTERN_WHITESPACE.split(in);
            for (final String s : bb) {
                final String[] bbb = PATTERN_CARAT.split(s);
                out.put(bbb[0], 1 == bbb.length ? defaultBoost : Float.valueOf(bbb[1]));
            }
        }
        return out;
    }

    /**
     * Copied from DisMaxQParser (as we don't handle user fields/aliases yet)
     *
     * Uses {@link SolrPluginUtils#parseFieldBoosts(String)} with the 'qf'
     * parameter. Falls back to the 'df' parameter.
     *
     * @param fieldParamName The name of the request parameter from which to read the query fields and boostings
     * @param defaultBoost The default field weight
     * @param useDefaultFieldAsFallback Iff true, search in field {@link CommonParams}.DF if the specified request parameter is empty
     * @return A mapping between query fields and their boost factor.
     *
     */
    protected Map<String, Float> parseQueryFields(final String fieldParamName, final Float defaultBoost,
                                                      final boolean useDefaultFieldAsFallback) {

        final Map<String, Float> queryFields = parseFieldBoosts(getRequestParams(fieldParamName), defaultBoost);
        if (queryFields.isEmpty() && useDefaultFieldAsFallback) {
            final String df = solrParams.get(CommonParams.DF);
            if (df == null) {
                throw new RuntimeException("Neither " + fieldParamName + ", " + CommonParams.DF + ", nor the default " +
                        "search field are present.");
            }
            queryFields.put(df, defaultBoost);
        }
        return queryFields;
    }


    protected static PhraseBoostFieldParams fieldParams2phraseBoostFieldParams(final FieldParams fieldParams) {
        final PhraseBoosting.NGramType nGramType;
        switch (fieldParams.getWordGrams()) {
            case 0: nGramType = PhraseBoosting.NGramType.PHRASE; break;
            case 2: nGramType = PhraseBoosting.NGramType.BI_GRAM; break;
            case 3: nGramType = PhraseBoosting.NGramType.TRI_GRAM; break;
            default:
                throw new IllegalArgumentException("Unknown wordGrams: " + fieldParams.getWordGrams());
        }
        return new PhraseBoostFieldParams(fieldParams.getField(), nGramType, fieldParams.getSlop(),
                fieldParams.getBoost());

    }


    private Optional<QuerySimilarityScoring> getSimilarityScoringParam(final String paramName) {

        return getRequestParam(paramName).map(scoring -> {
            switch (scoring) {
                case SIMILARITY_SCORE_DFC:
                    return QuerySimilarityScoring.DFC;
                case SIMILARITY_SCORE_OFF:
                    return QuerySimilarityScoring.SIMILARITY_SCORE_OFF;
                case SIMILARITY_SCORE_ON:
                    return QuerySimilarityScoring.SIMILARITY_SCORE_ON;
                default:
                    throw new IllegalArgumentException("Invalid value for " + paramName + ": " + scoring);
            }
        });


    }

    public boolean isFieldPhraseQueryable(final SchemaField field) {
        if (field != null) {
            final FieldType fieldType = field.getType();
            return (fieldType instanceof TextField) && !field.omitPositions() && !field.omitTermFreqAndPositions();
        }
        return false;
    }

    @Override
    public Optional<String> getRequestParam(final String name) {
        return Optional.ofNullable(solrParams.get(name));
    }

    @Override
    public String[] getRequestParams(final String name) {
        final String[] params = solrParams.getParams(name);
        return params == null ? EMPTY_STRING_ARRAY : params;

    }

    @Override
    public Optional<Boolean> getBooleanRequestParam(final String name) {
        return Optional.ofNullable(solrParams.getBool(name));
    }

    @Override
    public Optional<Integer> getIntegerRequestParam(final String name) {
        return Optional.ofNullable(solrParams.getInt(name));
    }

    @Override
    public Optional<Float> getFloatRequestParam(final String name) {
        return Optional.ofNullable(solrParams.getFloat(name));
    }

    @Override
    public Optional<Double> getDoubleRequestParam(final String name) {
        return Optional.ofNullable(solrParams.getDouble(name));
    }

    @Override
    public RewriteLoggingConfig getRewriteLoggingConfig() {
        return rewriteLoggingConfig;
    }

}
