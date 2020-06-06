package querqy.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.lucene.rewrite.SearchFieldsAndBoosting.FieldBoostModel;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.model.ExpandedQuery;
import querqy.model.QuerqyQuery;
import querqy.model.RawQuery;
import querqy.parser.QuerqyParser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This interface defines methods to access the search request in the context of the given search engine
 * and of Lucene query building.
 *
 * It is mainly used to hide search engine specifics away from the {@link QueryParsingController}.
 *
 */
public interface LuceneSearchEngineRequestAdapter extends SearchEngineRequestAdapter {

    /**
     * <p>Get the query string that should be parsed into the main query.</p>
     *
     * <p>Must be neither null or nor empty.</p>
     *
     * @return The query string.
     */
    String getQueryString();

    /**
     * <p>Does this query string mean 'match all documents'?</p>
     * @param queryString The query string.
     * @return true if the  query string means 'match all documents' and false otherwise
     */
    boolean isMatchAllQuery(String queryString);

    /**
     * <p>Should the query results be scored?</p>
     *
     * <p>This should return false for filter queries. If this method returns false, no boost queries will be used
     * (neither those from Querqy query rewriting nor those that were passed in request parameters).</p>
     *
     * @return true if the query results should be scored and false otherwise
     */
    boolean needsScores();

    /**
     * <p>Get the analyzer for applying text analysis to query terms.</p>
     *
     * <p>This will normally be an {@link Analyzer} that delegates to other Analyzers based on the given query fields.</p>
     * @return The query analyzer.
     */
    Analyzer getQueryAnalyzer();

    /**
     * Get an optional {@link TermQueryCache}
     *
     * @return The optional TermQueryCache
     */
    Optional<TermQueryCache> getTermQueryCache();

    /**
     * <p>Should Querqy boost queries be added to the main query?</p>
     *
     * <p>If this method returns true, {@link QueryParsingController#process()} will include boost queries that were
     * created during query rewriting as optional clauses of the returned {@link LuceneQueries#mainQuery}.
     * If it returns false, the caller of {@link QueryParsingController#process()} will be responsible for applying
     * the boost queries, for example in a re-rank query. In this case, Querqy boost queries will not be included in
     * the {@link LuceneQueries#mainQuery} but be returned in {@link LuceneQueries#querqyBoostQueries}</p>
     *
     * @return true if Querqy boost queries should be added to the main query and false otherwise
     */
    boolean addQuerqyBoostQueriesToMainQuery();

    /**
     * <p>Get the {@link QuerySimilarityScoring} to be used in the user query and in boost queries that are passed as
     * request parameters.</p>
     *
     * <p>If this method returns an empty {@link Optional},
     * {@link QueryParsingController#DEFAULT_USER_QUERY_SIMILARITY_SCORING} will be used.</p>
     *
     * @return An optional QuerySimilarityScoring
     */
    Optional<QuerySimilarityScoring> getUserQuerySimilarityScoring();

    /**
     * <p>Get the {@link QuerySimilarityScoring} to be used in boost queries that are created during query rewriting.</p>
     *
     * <p>If this method returns an empty {@link Optional},
     * {@link QueryParsingController#DEFAULT_BOOST_QUERY_SIMILARITY_SCORING} will be used.</p>
     *
     * @return An optional QuerySimilarityScoring
     */
    Optional<QuerySimilarityScoring> getBoostQuerySimilarityScoring();



    /**
     * <p>Get the query fields and their weights for the query entered by the user.</p>
     *
     * @see #getGeneratedQueryFieldsAndBoostings()
     * @return A map of field names and field boost factors.
     */
    Map<String, Float> getQueryFieldsAndBoostings();

    /**
     *  <p>Get the query fields and their weights for queries that were generated during query rewriting.</p>
     *
     *  <p>If this method returns an empty map, the map returned by {@link #getQueryFieldsAndBoostings()} will also be
     *  used for generated queries.</p>
     *
     *  @see #useFieldBoostingInQuerqyBoostQueries()
     *
     * @return A map of field names and field boost factors, or an empty map.
     */
    Map<String, Float> getGeneratedQueryFieldsAndBoostings();

    /**
     * <p>Create a {@link QuerqyParser} for parsing the user query string.</p>
     *
     * <p>If an empty Optional is returned, an instance of {@link QueryParsingController#DEFAULT_PARSER_CLASS}
     * will be used.</p>
     *
     * @return An optional QuerqyParser
     */
    Optional<QuerqyParser> createQuerqyParser();

    /**
     * <p>Should field boosts be applied to boost queries that were generated by Querqy query rewriting?</p>
     * <p>If this method returns false, all field boosts will be set to 1f for the generated boost queries, otherwise
     * the values produced by getGeneratedQueryFieldsAndBoostings and {@link #getGeneratedFieldBoost()} will be used.</p>
     *
     * @return true of field boosts should be used for generated boost queries, false otherwise
     */
    boolean useFieldBoostingInQuerqyBoostQueries();


    /**
     * <p>Get an optional tiebreaker.</p>
     * <p>Query fields and their weights will be applied to the terms of the user query and to queries that were
     * generated at query rewriting. When scoring the documents in the result set, this tiebreaker controls how much
     * weight is given (per term) to the highest scoring field match in a given document vs all other fields. A value of
     * 0.0 will only use the score from the highest scoring field. A value of 1.0 would add up all scores from all
     * fields.</p>
     * <p>If an empty Optional is returned, {@link QueryParsingController#DEFAULT_TIEBREAKER} will be used.</p>
     *
     * @return An optional tiebreaker.
     */
    Optional<Float> getTiebreaker();

    /**
     * <p>Apply the 'minimum should match' setting of the request.</p>
     * <p>It will be the responsibility of the LuceneSearchEngineRequestAdapter implementation to derive the
     * 'minimum should match' setting from request parameters or other configuration.</p>
     * <p>The query parameter is the rewritten user query. {@link querqy.rewrite.QueryRewriter}s shall guarantee to
     * preserve the number of top-level query clauses at query rewriting.</p>
     *
     * @see BooleanQuery#getMinimumNumberShouldMatch()
     * @param query The parsed and rewritten user query.
     *
     * @return The query after application of 'minimum should match'
     */
    Query applyMinimumShouldMatch(BooleanQuery query);


    /**
     * Get the weight to be multiplied with the main Querqy query (the query entered by the user).
     *
     * @return
     */
    Optional<Float> getUserQueryWeight();

    /**
     * <p>Get an optional weight that will be multiplied with the field boosts of all generated query fields.</p>
     * <p>The idea behind this option is to provide a global setting that allows to quickly change the weights of all
     * queries that were created at query rewriting. This factor is multiplied with the field boosts that are provided
     * by {@link #getGeneratedQueryFieldsAndBoostings()}.</p>
     *
     * <p>If an empty Optional is returned, {@link QueryParsingController#DEFAULT_GENERATED_FIELD_BOOST} will be used.</p>
     *
     * @see #useFieldBoostingInQuerqyBoostQueries()
     * @see #getGeneratedQueryFieldsAndBoostings()
     * @return An optional factor that is multiplied with the field boosts of generated query fields.
     *
     */
    Optional<Float> getGeneratedFieldBoost();

    /**
     * <p>Get the weight to be multiplied with Querqy boost queries.</p>
     * <p>If an empty Optional is returned, {@link QueryParsingController#DEFAULT_POSITIVE_QUERQY_BOOST_WEIGHT} will
     * be used.</p>
     * @see ExpandedQuery#getBoostUpQueries()
     *
     * @return The weight for positive boost queries.
     */
    Optional<Float> getPositiveQuerqyBoostWeight();

    /**
     *
     * <p>Get the weight to be multiplied with Querqy negative boost queries.</p>
     * <p>The returned weight will be used as Math.abs(getNegativeQuerqyBoostWeight()) * -1f</p>
     * <p>If an empty Optional is returned, {@link QueryParsingController#DEFAULT_NEGATIVE_QUERQY_BOOST_WEIGHT} will
     * be used.</p>
     * @see ExpandedQuery#getBoostDownQueries()
     *
     * @return The weight for negative boost queries.
     */
    Optional<Float> getNegativeQuerqyBoostWeight();

    /**
     * <p>Get the list of boost queries whose scores should be added to the score of the main query.</p>
     * <p>The queries are not a result of query rewriting but queries that may have been added as request parameters
     * (like 'bq' in Solr's Dismax query parser).</p>
     * @param userQuery The user query parsed into a {@link QuerqyQuery}
     * @return The list of additive boost queries or an empty list if no such query exists.
     * @throws SyntaxException if a multiplicative boost query could not be parsed
     *
     */
    List<Query> getAdditiveBoosts(final QuerqyQuery<?> userQuery) throws SyntaxException;

    /**
     * <p>Get the list of boost queries whose scores should be multiplied to the score of the main query.</p>
     * <p>The queries are
     * not a result of query rewriting but queries that may have been added as request parameters (like 'boost'
     * in Solr's Extended Dismax query parser).</p>
     *
     * @param userQuery The user query parsed into a {@link QuerqyQuery}
     * @return The list of multiplicative boost queries or an empty list if no such query exists.
     * @throws SyntaxException if a multiplicative boost query could not be parsed
     */
    List<Query> getMultiplicativeBoosts(final QuerqyQuery<?> userQuery) throws SyntaxException;

    /**
     * <p>Parse a {@link RawQuery}.</p>
     *
     * @param rawQuery The raw query.
     * @return The Query parsed from {@link RawQuery} if the class extending {@link RawQuery} is known
     * @throws SyntaxException @throws SyntaxException if the raw query query could not be parsed
     */
    Query parseRawQuery(RawQuery rawQuery) throws SyntaxException;


    /**
     * <p>Get the field boost model to use.</p>
     * <p>In Querqy, field boosting is modelled separately from
     * {@link org.apache.lucene.search.similarities.Similarity}. It applies to the rewritten user query, including
     * boost queries that were generated by Querqy.</p>
     * <p>This method decides which {@link FieldBoostModel} should be used in the current search request. If it returns
     * an empty Optional, {@link QueryParsingController#DEFAULT_FIELD_BOOST_MODEL} will be used.</p>
     *
     * @return The field boost model.
     */
    Optional<FieldBoostModel> getFieldBoostModel();



    class SyntaxException extends Exception {
        public SyntaxException() {
            super();
        }

        public SyntaxException(String message) {
            super(message);
        }

        public SyntaxException(String message, Throwable cause) {
            super(message, cause);
        }

        public SyntaxException(Throwable cause) {
            super(cause);
        }

        protected SyntaxException(String message, Throwable cause, boolean enableSuppression,
                                  boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }
}
