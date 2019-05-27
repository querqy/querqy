package querqy.solr;


public interface QuerqyDismaxParams {


    /**
     * Does the query need scores?
     */
    String NEEDS_SCORES = "needsScores";

    /**
     * generated field boost
     */
    String GFB                      = "gfb";

    /**
     * generated query fields (query generated terms only in these fields)
     */
    String GQF                      = "gqf";

    /**
     * field boost model
     */
    String FBM                      = "fbm";

    /**
     * Query-independent field boost model, reading field boost factors from request params
     */
    String FBM_FIXED                = "fixed";

    /**
     * Query-dependent field boost model ('Probabilistic Retrieval Model for Semi-structured Data')
     */
    String FBM_PRMS                 = "prms";

    /**
     * boost method - the method to integrate Querqy boost queries with the main query
     */
    String QBOOST_METHOD = "qboost.method";

    /**
     *  Integrate Querqy boost queries with the main query as a re-rank query
     */
    String QBOOST_METHOD_RERANK = "rerank";

    /**
     * The number of docs in the main query result to use for re-ranking when qboost.method=rerank
     */
    String QBOOST_RERANK_NUMDOCS = "qboost.rerank.numDocs";

    /**
     * Add Querqy boost queries to the main query as optional boolean clauses
     */
    String QBOOST_METHOD_OPT = "opt";

    /**
     * This parameter controls whether field boosting should be applied to Querqy boost queries.
     */
    String QBOOST_FIELD_BOOST = "qboost.fieldBoost";

    /**
     * A possible value of QBOOST_FIELD_BOOST: Do not apply any field boosting to Querqy boost queries.
     */
    String QBOOST_FIELD_BOOST_OFF = "off";

    /**
     * A possible value of QBOOST_FIELD_BOOST: Use the field boosting for Querqy boost queries.
     */
    String QBOOST_FIELD_BOOST_ON = "on";

    /**
     * Tie parameter for combining pf, pf2 and pf3 phrase boostings into a dismax query.
     */
    String QPF_TIE = "qpf.tie";

    /**
     * A possible value of {@link #QBOOST_SIMILARITY_SCORE}: Do not calculate the similarity score for Querqy boost
     * queries.
     * As a result the boost queries are only scored by query boost and field boost but not by any function of DF or TF.
     * Setting qboost.similarityScore=off yields a small performance gain as TF and DF need not be provided.
     */
    String SIMILARITY_SCORE_OFF = "off";

    /**
     * A possible value of {@link #QBOOST_SIMILARITY_SCORE}: Just use the similarity as set in Solr when scoring Querqy
     * boost queries.
     */
    String SIMILARITY_SCORE_ON = "on";
    /**
     * A possible value of {@link #QBOOST_SIMILARITY_SCORE}: "document frequency correction" - use the similarity as set
     * in Solr when scoring Querqy boost queries but fake the document frequency so that all term queries under a given
     * {@link org.apache.lucene.search.DisjunctionMaxQuery} us the same document frequency. This avoids situations
     * in which the rarer of two synonymous terms would get a higher score than the more common term. It also fixes
     * the IDF problem for the same term value occurring in two or more different fields with different frequencies.
     *
     */
    String SIMILARITY_SCORE_DFC = "dfc";
    /**
     * A boost factor to be applied to the query as entered by the user (the value of parameter q). This boost factor
     * allows to weight the user query against Querqy boost queries but also against the queries derived from pf, pf2,
     * pf3, boost, bf and bq. Default: 1.0.
     */
    String USER_QUERY_BOOST = "uq.boost";
    /**
     * Control how the score resulting from the {@link org.apache.lucene.search.similarities.Similarity}
     * implementation is integrated into the score of the user query (the value of parameter q).
     * Accepts "off" (use only field weights), "on" (use standard Lucene similarity) and
     * "dfc" (correct document frequency across fields). Default: "dfc"
     */
    String USER_QUERY_SIMILARITY_SCORE = "uq.similarityScore";
    /**
     * Control how the score resulting from the {@link org.apache.lucene.search.similarities.Similarity}
     * implementation is integrated into the score of a Querqy boost query
     */
    String QBOOST_SIMILARITY_SCORE = "qboost.similarityScore";
    /**
     * A global weight factor to be applied to positive Querqy boost queries (multiplied with the boosts of individual
     * positive boost queries.) Default: 1.0.
     */
    String QBOOST_WEIGHT = "qboost.weight";
    /**
     * A global weight factor to be applied to negative Querqy boost queries (multiplied with the boosts of individual
     * negative boost queries.) Default: 1.0.
     */
    String QBOOST_NEG_WEIGHT = "qboost.negWeight";

    /**
     * Same as {@link org.apache.solr.common.params.DisMaxParams#PS}
     */
    String PS = "ps";

    /**
     * Same as {@link org.apache.solr.common.params.DisMaxParams#PS2}
     */
    String PS2 = "ps2";

    /**
     * Same as {@link org.apache.solr.common.params.DisMaxParams#PS3}
     */
    String PS3 = "ps3";


    /**
     * Same as {@link org.apache.solr.common.params.DisMaxParams#PF}
     */
    String PF = "pf";

    /**
     * Same as {@link org.apache.solr.common.params.DisMaxParams#PF2}
     */
    String PF2 = "pf2";

    /**
     * Same as {@link org.apache.solr.common.params.DisMaxParams#PF3}
     */
    String PF3 = "pf3";

    /**
     * Same as {@link org.apache.solr.common.params.DisMaxParams#TIE}
     */
    String TIE = "tie";

    /**
     * Same as {@link org.apache.solr.common.params.DisMaxParams#QF}
     */
    String QF = "qf";

    /**
     * Same as {@link org.apache.solr.common.params.DisMaxParams#BQ}
     */
    String BQ = "bq";

    /**
     * Same as {@link org.apache.solr.common.params.DisMaxParams#BF}
     */
    String BF = "bf";

    /**
     * A multiplicative boost query.
     */
    String MULT_BOOST = "boost";

    /**
     * Turn info logging on/off. Default = 'off'
     */
    String INFO_LOGGING = "querqy.infoLogging";
}
