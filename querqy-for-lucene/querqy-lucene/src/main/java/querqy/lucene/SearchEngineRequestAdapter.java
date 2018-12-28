package querqy.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;
import querqy.lucene.rewrite.SearchFieldsAndBoosting.FieldBoostModel;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.model.RawQuery;
import querqy.parser.QuerqyParser;
import querqy.rewrite.RewriteChain;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by rene on 23/05/2017.
 */
public interface SearchEngineRequestAdapter {

    List<Query> EMPTY_QUERY_LIST = Collections.emptyList();

    String getQueryString();
    boolean isMatchAllQuery(String queryString);

    boolean needsScores();
    Analyzer getQueryAnalyzer();
    TermQueryCache getTermQueryCache();

    boolean useReRankQueryForBoosting();

    QuerySimilarityScoring getUserQuerySimilarityScoring();
    QuerySimilarityScoring getBoostQuerySimilarityScoring();
    Map<String, Float> getQueryFieldsAndBoostings();

    List<PhraseBoostFieldParams> getQueryablePhraseBoostFieldParams();
    Optional<Float> getPhraseBoostTiebreaker();


    Optional<QuerqyParser> createQuerqyParser();

    RewriteChain getRewriteChain();

    Map<String, Object> getContext();

    boolean useFieldBoostingInQuerqyBoostQueries();


    Optional<Float> getTiebreaker();
    Query applyMinimumShouldMatch(Query query);

    boolean isDebugQuery();


    /**
     * Get the weight to be multiplied with the main Querqy query (the query entered by the user).
     *
     * @return
     */
    Optional<Float> getUserQueryWeight();

    Optional<Float> getGeneratedFieldBoost();

    /**
     * Get the weight to be multiplied with Querqy boost queries.
     *
     * @return
     */
    Optional<Float> getBoostQueryWeight();

    /**
     *
     * Get the weight to be multiplied with Querqy negative boost queries. The returned weight will be used as
     * Math.abs(getNegativeBoostQueryWeight()) * -1f
     *
     * @return
     */
    Optional<Float> getNegativeBoostQueryWeight();

    /**
     * Get the list of boost queries whose scores should be added to the score of the main query. The queries are
     * not a result of query rewriting but queries that may have been added as request parameters (like 'bq' in Solr's
     * Dismax query parser)
     *
     * @return The list of additive boost queries or an empty list if no such query exists.
     */
    List<Query> getAdditiveBoosts() throws SyntaxException;

    /**
     * Get the list of boost queries whose scores should be multiplied to the score of the main query. The queries are
     * not a result of query rewriting but queries that may have been added as request parameters (like 'boost'
     * in Solr's Extended Dismax query parser).
     */
    List<Query> getMultiplicativeBoosts() throws SyntaxException;

    Query parseRawQuery(RawQuery rawQuery) throws SyntaxException;




    /**
     *
     * @return Fields and boost factors to use for generated fields or None to use value from
     * {@link #getQueryFieldsAndBoostings()}
     */
    Map<String, Float> getGeneratedQueryFieldsAndBoostings();


    FieldBoostModel getFieldBoostModel();



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
