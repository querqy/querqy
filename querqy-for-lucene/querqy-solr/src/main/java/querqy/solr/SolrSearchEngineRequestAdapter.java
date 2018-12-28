package querqy.solr;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
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
import org.apache.solr.search.SyntaxError;
import org.apache.solr.util.SolrPluginUtils;
import querqy.lucene.PhraseBoostFieldParams;
import querqy.lucene.QuerySimilarityScoring;
import querqy.lucene.SearchEngineRequestAdapter;
import querqy.lucene.rewrite.SearchFieldsAndBoosting;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.model.RawQuery;
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

public class SolrSearchEngineRequestAdapter implements SearchEngineRequestAdapter {


    public static final String NEEDS_SCORES = "needsScores";

    /**
     * generated field boost
     */
    public static final String GFB                      = "gfb";

    /**
     * generated query fields (query generated terms only in these fields)
     */
    public static final String GQF                      = "gqf";


    /**
     * field boost model
     */
    public static final String FBM                      = "fbm";

    /**
     * Query-independent field boost model, reading field boost factors from request params
     */
    public static final String FBM_FIXED                = "fixed";

    /**
     * Query-dependent field boost model ('Probabilistic Retrieval Model for Semi-structured Data')
     */
    public static final String FBM_PRMS                 = "prms";

    /**
     * The default field boost model (= {@link #FBM_FIXED})
     */
    public static final String FBM_DEFAULT              = FBM_FIXED;


    /**
     * boost method - the method to integrate Querqy boost queries with the main query
     */
    public static final String QBOOST_METHOD = "qboost.method";

    /**
     *  Integrate Querqy boost queries with the main query as a re-rank query
     */
    public static final String QBOOST_METHOD_RERANK = "rerank";


    /**
     * The number of docs in the main query result to use for re-ranking when qboost.method=rerank
     */
    public static final String QBOOST_RERANK_NUMDOCS = "qboost.rerank.numDocs";

    /**
     * The default value for {@link #QBOOST_RERANK_NUMDOCS}
     */
    public static final int DEFAULT_RERANK_NUMDOCS      = 500;

    /**
     * Add Querqy boost queries to the main query as optional boolean clauses
     */
    public static final String QBOOST_METHOD_OPT = "opt";

    /**
     * The default boost method (= {@link #QBOOST_METHOD})
     */
    public static final String QBOOST_METHOD_DEFAULT = QBOOST_METHOD_OPT;


    /**
     * This parameter controls whether field boosting should be applied to Querqy boost queries.
     */
    public static final String QBOOST_FIELD_BOOST = "qboost.fieldBoost";

    /**
     * A possible value of QBOOST_FIELD_BOOST: Do not apply any field boosting to Querqy boost queries.
     */
    public static final String QBOOST_FIELD_BOOST_OFF = "off";

    /**
     * A possible value of QBOOST_FIELD_BOOST: Use the field boosting as set by parameter {@link #FBM} for Querqy boost
     * queries.
     */
    public static final String QBOOST_FIELD_BOOST_ON = "on";

    public static final String DEFAULT_QBOOST_FIELD_BOOST = QBOOST_FIELD_BOOST_ON;


    /**
     * Tie parameter for combining pf, pf2 and pf3 phrase boostings into a dismax query. Defaults to the value
     * of the {@link org.apache.solr.common.params.DisMaxParams#TIE} parameter
     */
    public static final String QPF_TIE = "qpf.tie";


    /*
     *
     * Similarity scoring
     *
     */


    /**
     * A possible value of QBOOST_SIMILARITY_SCORE: Do not calculate the similarity score for Querqy boost queries.
     * As a result the boost queries are only scored by query boost and field boost but not by any function of DF or TF.
     * Setting qboost.similarityScore=off yields a small performance gain as TF and DF need not be provided.
     */
    public static final String SIMILARITY_SCORE_OFF = "off";

    /**
     * A possible value of QBOOST_SIMILARITY_SCORE: Just use the similarity as set in Solr when scoring Querqy boost queries.
     */
    public static final String SIMILARITY_SCORE_ON = "on";

    /**
     * A possible value of QBOOST_SIMILARITY_SCORE: "document frequency correction" - use the similarity as set in Solr
     * when scoring Querqy boost queries but fake the document frequency so that all term queries under a given
     * {@link org.apache.lucene.search.DisjunctionMaxQuery} us the same document frequency. This avoids situations
     * in which the rarer of two synonymous terms would get a higher score than the more common term. It also fixes
     * the IDF problem for the same term value occurring in two or more different fields with different frequencies.
     *
     */
    public static final String SIMILARITY_SCORE_DFC = "dfc";


    /**
     * A boost factor to be applied to the query as entered by the user (the value of parameter q). This boost factor
     * allows to weight the user query against Querqy boost queries but also against the queries derived from pf, pf2,
     * pf3, boost, bf and bq. Default: 1.0.
     */
    public static final String USER_QUERY_BOOST = "uq.boost";

    /**
     * Control how the score resulting from the {@link org.apache.lucene.search.similarities.Similarity}
     * implementation is integrated into the score of the user query (the value of parameter q).
     * Accepts "off" (use only field weights), "on" (use standard Lucene similarity) and
     * "dfc" (correct document frequency across fields). Default: "dfc"
     */
    public static final String USER_QUERY_SIMILARITY_SCORE = "uq.similarityScore";

    public static final String DEFAULT_USER_QUERY_SIMILARITY = SIMILARITY_SCORE_DFC;

    /**
     * Control how the score resulting from the {@link org.apache.lucene.search.similarities.Similarity}
     * implementation is integrated into the score of a Querqy boost query
     */
    public static final String QBOOST_SIMILARITY_SCORE = "qboost.similarityScore";

    public static final String DEFAULT_QBOOST_SIMILARITY_SCORE = SIMILARITY_SCORE_DFC;


    /*
     *
     * end - similarity scoring
     *
     */

    /**
     * A global weight factor to be applied to positive Querqy boost queries (multiplied with the boosts of individual
     * positive boost queries.) Default: 1.0.
     */
    public static final String QBOOST_WEIGHT = "qboost.weight";

    /**
     * A global weight factor to be applied to negative Querqy boost queries (multiplied with the boosts of individual
     * negative boost queries.) Default: 1.0.
     */
    public static final String QBOOST_NEG_WEIGHT = "qboost.negWeight";


    /**
     *
     * (E)Dismax
     *
     *
     */
    public static String MULT_BOOST = "boost";

    /*
     *
     * end - (E)Dismax
     *
     */

    private static final Pattern PATTERN_WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern PATTERN_CARAT = Pattern.compile("\\^");


    private static final String MATCH_ALL = "*:*";



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

    public SolrSearchEngineRequestAdapter(final QParser qParser, final SolrQueryRequest request,
                                          final String queryString, final SolrParams solrParams,
                                          final QuerqyParser querqyParser, final RewriteChain rewriteChain,
                                          final TermQueryCache termQueryCache) {
        this.qParser = qParser;
        this.userQueryString = queryString;
        this.solrParams = solrParams;
        this.termQueryCache = termQueryCache;
        this.querqyParser = querqyParser;
        this.request = request;
        this.rewriteChain = rewriteChain;
        this.context = new HashMap<>();

        final int ps0 = solrParams.getInt(DisMaxParams.PS, 0);
        final int ps2 = solrParams.getInt(DisMaxParams.PS, ps0);
        final int ps3 = solrParams.getInt(DisMaxParams.PS, ps0);


        final List<FieldParams> phraseFields = SolrPluginUtils
                .parseFieldBoostsAndSlop(solrParams.getParams(DisMaxParams.PF),0,ps0);
        final List<FieldParams> phraseFields2 = SolrPluginUtils
                .parseFieldBoostsAndSlop(solrParams.getParams(DisMaxParams.PF2),2,ps2);
        final List<FieldParams> phraseFields3 = SolrPluginUtils
                .parseFieldBoostsAndSlop(solrParams.getParams(DisMaxParams.PF3),3,ps3);

        allPhraseFields = new ArrayList<>(phraseFields.size() + phraseFields2.size() + phraseFields3.size());
        allPhraseFields.addAll(phraseFields);
        allPhraseFields.addAll(phraseFields2);
        allPhraseFields.addAll(phraseFields3);

        minShouldMatch = DisMaxQParser.parseMinShouldMatch(request.getSchema(), solrParams);

    }

    @Override
    public String getQueryString() {
        return userQueryString;
    }

    @Override
    public boolean needsScores() {
        return solrParams.getBool(NEEDS_SCORES, true);
    }

    @Override
    public TermQueryCache getTermQueryCache() {
        return termQueryCache;
    }

    @Override
    public boolean useReRankQueryForBoosting() {
        return QBOOST_METHOD_RERANK.equals(solrParams.get(QBOOST_METHOD, QBOOST_METHOD_DEFAULT));
    }

    public int getReRankNumDocs() {
        return solrParams.getInt(QBOOST_RERANK_NUMDOCS, DEFAULT_RERANK_NUMDOCS);
    }

    @Override
    public QuerySimilarityScoring getUserQuerySimilarityScoring() {
        return getSimilarityScoringParam(USER_QUERY_SIMILARITY_SCORE, DEFAULT_USER_QUERY_SIMILARITY);
    }

    @Override
    public Optional<Float> getUserQueryWeight() {
        return Optional.ofNullable(solrParams.getFloat(USER_QUERY_BOOST));
    }

    @Override
    public QuerySimilarityScoring getBoostQuerySimilarityScoring() {
        return getSimilarityScoringParam(QBOOST_SIMILARITY_SCORE, DEFAULT_QBOOST_SIMILARITY_SCORE);
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
        return Optional.ofNullable(solrParams.getFloat(DisMaxParams.TIE));
    }

    @Override
    public Optional<Float> getPhraseBoostTiebreaker() {
        return Optional.ofNullable(solrParams.getFloat(QPF_TIE));
    }

    @Override
    public boolean isDebugQuery() {
        return solrParams.getBool(CommonParams.DEBUG_QUERY, false);
    }

    @Override
    public Optional<QuerqyParser> createQuerqyParser() {
        return Optional.ofNullable(querqyParser);
    }

    @Override
    public List<PhraseBoostFieldParams> getQueryablePhraseBoostFieldParams() {

        final IndexSchema schema = request.getSchema();

        return allPhraseFields.stream()
                .filter(field -> isFieldPhraseQueryable(schema.getFieldOrNull(field.getField())))
                .map(solrField -> new PhraseBoostFieldParams(
                        solrField.getField(),
                        solrField.getWordGrams(),
                        solrField.getSlop(),
                        solrField.getBoost()))
                .collect(Collectors.toList());

    }

    @Override
    public RewriteChain getRewriteChain() {
        return rewriteChain;
    }

    @Override
    public boolean isMatchAllQuery(final String queryString) {
        return (userQueryString.charAt(0) == '*')
                && (userQueryString.length() == 1 || MATCH_ALL.equals(userQueryString));
    }

    @Override
    public Optional<Float> getBoostQueryWeight() {
        return Optional.ofNullable(solrParams.getFloat(QBOOST_WEIGHT));
    }

    @Override
    public Optional<Float> getNegativeBoostQueryWeight() {
        return Optional.ofNullable(solrParams.getFloat(QBOOST_NEG_WEIGHT));
    }

    @Override
    public Map<String, Float> getQueryFieldsAndBoostings() {
        return parseQueryFields( DisMaxParams.QF, 1f, true);
    }

    @Override
    public Map<String, Object> getContext() {
        return context;
    }

    @Override
    public Analyzer getQueryAnalyzer() {
        return request.getSchema().getQueryAnalyzer();
    }

    @Override
    public Query parseRawQuery(final RawQuery rawQuery) throws SyntaxException {
        try {
            return QParser.getParser(rawQuery.getQueryString(), null, request).getQuery();
        } catch (SyntaxError syntaxError) {
            throw new SyntaxException(syntaxError);
        }
    }

    @Override
    public Query applyMinimumShouldMatch(final Query query) {
        if (!(query instanceof BooleanQuery)) {
            return query;
        }

        final BooleanQuery bq = (BooleanQuery) query;
        final List<BooleanClause> clauses = bq.clauses();
        if (clauses.size() < 2) {
            return bq;
        }

        for (final BooleanClause clause : clauses) {
            if ((clause.getQuery() instanceof BooleanQuery) && (clause.getOccur() != BooleanClause.Occur.MUST)) {
                return bq; // seems to be a complex query with sub queries - do not
                // apply mm
            }
        }

        return SolrPluginUtils.setMinShouldMatch(bq, minShouldMatch);

    }



    @Override
    public Map<String, Float> getGeneratedQueryFieldsAndBoostings()  {
        return parseQueryFields(GQF, null, false);
    }

    @Override
    public Optional<Float> getGeneratedFieldBoost() {
        return Optional.ofNullable(solrParams.getFloat(GFB));
    }

    @Override
    public SearchFieldsAndBoosting.FieldBoostModel getFieldBoostModel() {

        final String fbm = solrParams.get(FBM, FBM_DEFAULT);
        switch (fbm) {
            case FBM_FIXED: return SearchFieldsAndBoosting.FieldBoostModel.FIXED;
            case FBM_PRMS: return SearchFieldsAndBoosting.FieldBoostModel.PRMS;
            default: throw new IllegalArgumentException("Unknown field boost model: " + fbm);
        }

    }

    @Override
    public List<Query> getAdditiveBoosts() throws SyntaxException {

        final List<Query> boostQueries = parseQueriesFromParam(DisMaxParams.BQ, null);
        final String[] bfs = solrParams.getParams(DisMaxParams.BF);
        if (bfs == null || bfs.length ==0) {
            return boostQueries;
        }

        final List<Query> boosts = new ArrayList<>(boostQueries.size() + bfs.length);
        boosts.addAll(boostQueries);

        for (String bf : bfs) {
            if (bf != null && bf.trim().length()> 0) {
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
    public List<Query> getMultiplicativeBoosts() throws SyntaxException {
        return parseQueriesFromParam(MULT_BOOST, FunctionQParserPlugin.NAME);
    }

    private List<Query> parseQueriesFromParam(final String paramName, final String defaultParserName) throws SyntaxException {

        final String[] qStrings = solrParams.getParams(paramName);
        if (qStrings != null) {
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
     */
    protected Map<String, Float> parseQueryFields(final String fieldName, final Float defaultBoost,
                                                      final boolean useDfFallback) {

        final Map<String, Float> queryFields = parseFieldBoosts(solrParams.getParams(fieldName), defaultBoost);
        if (queryFields.isEmpty() && useDfFallback) {
            final String df = solrParams.get(CommonParams.DF);
            if (df == null) {
                throw new RuntimeException("Neither " + fieldName + ", " + CommonParams.DF + ", nor the default " +
                        "search field are present.");
            }
            queryFields.put(df, defaultBoost);
        }
        return queryFields;
    }

    private QuerySimilarityScoring getSimilarityScoringParam(final String paramName, final String defaultParamValue) {

        final String scoring = solrParams.get(paramName, defaultParamValue);
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
    }

    public boolean isFieldPhraseQueryable(final SchemaField field) {
        if (field != null) {
            final FieldType fieldType = field.getType();
            return (fieldType instanceof TextField) && !field.omitPositions() && !field.omitTermFreqAndPositions();
        }
        return false;
    }

}
