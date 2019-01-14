/**
 * 
 */
package querqy.solr;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queries.function.BoostedQuery;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.valuesource.ProductFloatFunction;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

import org.apache.lucene.util.QueryBuilder;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.TextField;
import org.apache.solr.search.*;

import org.apache.solr.util.SolrPluginUtils;

import querqy.lucene.LuceneQueryUtil;
import querqy.ComparableCharSequence;
import querqy.lucene.rewrite.LuceneTermQueryBuilder;
import querqy.lucene.rewrite.*;
import querqy.lucene.rewrite.SearchFieldsAndBoosting.FieldBoostModel;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.model.*;
import querqy.parser.QuerqyParser;
import querqy.rewrite.ContextAwareQueryRewriter;
import querqy.rewrite.RewriteChain;

/**
 * @author rene
 *
 */
@Slf4j
public class QuerqyDismaxQParser extends ExtendedDismaxQParser {

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

    public static final String NEEDS_SCORES = "needsScores";

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
     * Control how the score resulting from the {@link org.apache.lucene.search.similarities.Similarity}
     * implementation is integrated into the score of a Querqy boost query
     */
    public static final String QBOOST_SIMILARITY_SCORE = "qboost.similarityScore";

    /**
     * A possible value of QBOOST_SIMILARITY_SCORE: Do not calculate the similarity score for Querqy boost queries.
     * As a result the boost queries are only scored by query boost and field boost but not by any function of DF or TF.
     * Setting qboost.similarityScore=off yields a small performance gain as TF and DF need not be provided.
     */
    public static final String SIMILARITY_SCORE_OFF = "off";

    /**
     * Tie parameter for combining pf, pf2 and pf3 phrase boostings into a dismax query. Defaults to the value
     * of the {@link org.apache.solr.common.params.DisMaxParams.TIE} parameter
     */
    public static final String QPF_TIE = "qpf.tie";

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

    public static final String QBOOST_SIMILARITY_SCORE_DEFAULT = SIMILARITY_SCORE_DFC;

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

    public static final String QBOOST_FIELD_BOOST_DEFAULT = QBOOST_FIELD_BOOST_ON;

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



    public static final float DEFAULT_GQF_VALUE = Float.MIN_VALUE;

    static final String MATCH_ALL = "*:*";

    // the QF parsing code is copied from org.apache.solr.util.SolrPluginUtils
    // and
    // org.apache.solr.search.DisMaxQParser, replacing the default boost factor
    // null with 1f
    private static final Pattern whitespacePattern = Pattern.compile("\\s+");
    private static final Pattern caratPattern = Pattern.compile("\\^");

    final Analyzer queryAnalyzer;
    final RewriteChain rewriteChain;
    final QuerqyParser querqyParser;
    final Map<String, Float> userQueryFields;
    final Map<String, Float> generatedQueryFields;
    final LuceneQueryBuilder builder;
    final TermQueryBuilder boostTermQueryBuilder;
    final SearchFieldsAndBoosting boostSearchFieldsAndBoostings;

    protected List<Query> filterQueries = null;
    protected Map<String, Object> context = null;

    protected PublicExtendedDismaxConfiguration config = null;
    protected List<Query> boostQueries = null;

    protected final boolean useReRankForBoostQueries;
    protected final int reRankNumDocs;
    protected final TermQueryCache termQueryCache;

    protected final float qpfTie;

    protected final float boostQueryWeight;
    protected final float userQueryWeight;
    protected final float negativeBoostWeight;

    protected final boolean debugQuery;

    protected final boolean needsScores;

    protected Query userQuery = null;

    protected ExpandedQuery expandedQuery = null;

    private DocumentFrequencyCorrection dfc = null;

    public QuerqyDismaxQParser(final String qstr, final SolrParams localParams, final SolrParams params,
                               final SolrQueryRequest req, final RewriteChain rewriteChain,
                               final QuerqyParser querqyParser, final TermQueryCache termQueryCache)
         throws SyntaxError {

        super(qstr, localParams, params, req);

        this.querqyParser = querqyParser;
        this.termQueryCache = termQueryCache;

        if (config == null) {
            // this is a hack that works around ExtendedDismaxQParser keeping the
            // config member var private
            // and avoids calling createConfiguration() twice
            config = createConfiguration(qstr, localParams, params, req);
        }
        IndexSchema schema = req.getSchema();
        queryAnalyzer = schema.getQueryAnalyzer();
        this.rewriteChain = rewriteChain;
      
        SolrParams solrParams = SolrParams.wrapDefaults(localParams, params);
      
        userQueryFields = parseQueryFields(req.getSchema(), solrParams, DisMaxParams.QF, 1f, true);
        userQueryWeight = solrParams.getFloat(USER_QUERY_BOOST, 1f);
        boostQueryWeight = solrParams.getFloat(QBOOST_WEIGHT, 1f);
        negativeBoostWeight = Math.abs(solrParams.getFloat(QBOOST_NEG_WEIGHT, 1f));

        generatedQueryFields = parseQueryFields(req.getSchema(), solrParams, GQF, null, false);
        if (generatedQueryFields.isEmpty()) {
            for (final Map.Entry<String, Float> entry: userQueryFields.entrySet()) {
                generatedQueryFields.put(entry.getKey(), entry.getValue() * config.generatedFieldBoostFactor);
            }
        } else {
            for (final Map.Entry<String, Float> entry: generatedQueryFields.entrySet()) {
                if (entry.getValue() == null) {
                    final String name = entry.getKey();
                    final Float nonGeneratedBoostFactor = userQueryFields.getOrDefault(name, 1f);
                    entry.setValue(nonGeneratedBoostFactor * config.generatedFieldBoostFactor);
                }
            }
        }

        needsScores = solrParams.getBool(NEEDS_SCORES, true);

        final SearchFieldsAndBoosting searchFieldsAndBoosting =
                new SearchFieldsAndBoosting(getFieldBoostModelFromParam(solrParams),
                            userQueryFields, generatedQueryFields, config.generatedFieldBoostFactor);

        if (!needsScores) {
            useReRankForBoostQueries = false;
            dfc = null;
            boostTermQueryBuilder = null;
            boostSearchFieldsAndBoostings = null;
            qpfTie = 1f;
            reRankNumDocs = 0;
            builder = new LuceneQueryBuilder(new LuceneTermQueryBuilder(), queryAnalyzer, searchFieldsAndBoosting, 1f,
                    termQueryCache);

        } else {

            useReRankForBoostQueries = QBOOST_METHOD_RERANK.equals(solrParams.get(QBOOST_METHOD, QBOOST_METHOD_DEFAULT));
            if (useReRankForBoostQueries) {
                reRankNumDocs = solrParams.getInt(QBOOST_RERANK_NUMDOCS, DEFAULT_RERANK_NUMDOCS);
            } else {
                reRankNumDocs = 0;
            }


            final String boostSimScore = solrParams.get(QBOOST_SIMILARITY_SCORE, QBOOST_SIMILARITY_SCORE_DEFAULT);
            final String userQuerySimScore = solrParams.get(USER_QUERY_SIMILARITY_SCORE, SIMILARITY_SCORE_DFC);


            final TermQueryBuilder userTermQueryBuilder;
            if (SIMILARITY_SCORE_OFF.equals(userQuerySimScore)) {
                userTermQueryBuilder = new FieldBoostTermQueryBuilder();
            } else if (SIMILARITY_SCORE_ON.equals(userQuerySimScore)) {
                userTermQueryBuilder = new SimilarityTermQueryBuilder();
            } else {
                dfc = new DocumentFrequencyCorrection();
                userTermQueryBuilder = new DependentTermQueryBuilder(dfc);
            }

            builder = new LuceneQueryBuilder(userTermQueryBuilder,
                    queryAnalyzer, searchFieldsAndBoosting, config.getTieBreaker(), termQueryCache);



            if (SIMILARITY_SCORE_DFC.equals(boostSimScore)) {
                if (dfc == null) {
                    dfc = new DocumentFrequencyCorrection();
                }
                boostTermQueryBuilder = new DependentTermQueryBuilder(dfc);
            } else if (SIMILARITY_SCORE_ON.equals(boostSimScore)) {
                boostTermQueryBuilder = new SimilarityTermQueryBuilder();
            } else if (SIMILARITY_SCORE_OFF.equals(boostSimScore)) {
                boostTermQueryBuilder = new FieldBoostTermQueryBuilder();
            } else {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                        "Unknown similarity score handling for Querqy boost queries: " + boostSimScore);
            }

            final String boostFieldBoost = solrParams.get(QBOOST_FIELD_BOOST, QBOOST_FIELD_BOOST_DEFAULT);
            if (boostFieldBoost.equals(QBOOST_FIELD_BOOST_ON)) {
                boostSearchFieldsAndBoostings = searchFieldsAndBoosting;
            } else if (boostFieldBoost.equals(QBOOST_FIELD_BOOST_OFF)) {
                boostSearchFieldsAndBoostings = searchFieldsAndBoosting.withFieldBoostModel(FieldBoostModel.NONE);
            } else {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                        "Unknown field boost model for Querqy boost queries: " + boostFieldBoost);
            }

            qpfTie = solrParams.getFloat(QPF_TIE, config.getTieBreaker());

        }

        debugQuery = req.getParams().getBool(CommonParams.DEBUG_QUERY, false);

    }
   
   protected FieldBoostModel getFieldBoostModelFromParam(final SolrParams solrParams) {
       final String fbm = solrParams.get(FBM, FBM_DEFAULT);
       if ((!needsScores) || fbm.equals(FBM_FIXED)) {
           return FieldBoostModel.FIXED;
       } else if (fbm.equals(FBM_PRMS)) {
           return FieldBoostModel.PRMS;
       }
       
       throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, 
               "Unknown field boost model: " + fbm);
   }

   @Override
   protected PublicExtendedDismaxConfiguration createConfiguration(final String qstr, final SolrParams localParams,
                                                                   final SolrParams params,
                                                                   final SolrQueryRequest req) {
      // this is a hack that works around ExtendedDismaxQParser keeping the
      // config member var private
      // and avoids calling createConfiguration() twice
      this.config = new PublicExtendedDismaxConfiguration(localParams, params, req);// super.createConfiguration(qstr,
                                                                                    // localParams,
                                                                                    // params,
                                                                                    // req);
      return config;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.solr.search.QParser#parse()
    */
   @Override
   public Query parse() throws SyntaxError {
      // TODO q.alt
      String userQueryString = getString();
      if (userQueryString == null) {
         throw new SyntaxError("query string is null");
      }
      userQueryString = userQueryString.trim();
      if (userQueryString.length() == 0) {
         throw new SyntaxError("query string is empty");
      }

      Query mainQuery;
      List<Query> querqyBoostQueries = null;
      final Query phraseFieldQuery;

      if ((userQueryString.charAt(0) == '*') && (userQueryString.length() == 1 || MATCH_ALL.equals(userQueryString))) {
          mainQuery = new MatchAllDocsQuery();
          if (dfc != null) dfc.finishedUserQuery();
          phraseFieldQuery = null;
      } else {
          expandedQuery = makeExpandedQuery();
          updateRulesCriteria(expandedQuery);
          phraseFieldQuery = needsScores ? makePhraseFieldQueries(expandedQuery.getUserQuery()) : null;
          context = new HashMap<>();
          if (debugQuery) {
              context.put(ContextAwareQueryRewriter.CONTEXT_KEY_DEBUG_ENABLED, true);
          }
          expandedQuery = rewriteChain.rewrite(expandedQuery, context);
         
          mainQuery = makeUserQuery(expandedQuery);
         
          if (dfc != null) dfc.finishedUserQuery();
         
          applyFilterQueries(expandedQuery);
          querqyBoostQueries = needsScores ? getQuerqyBoostQueries(expandedQuery) : Collections.emptyList();

      }

      boostQueries = needsScores ? getBoostQueries() : Collections.emptyList();
      final List<Query> boostFunctions = needsScores ? getBoostFunctions() : Collections.emptyList();
      final List<ValueSource> multiplicativeBoosts = needsScores ? getMultiplicativeBoosts() : Collections.emptyList();

      final boolean hasMultiplicativeBoosts = multiplicativeBoosts != null && !multiplicativeBoosts.isEmpty();
      final boolean hasQuerqyBoostQueries = querqyBoostQueries != null && !querqyBoostQueries.isEmpty();

       // do we have to add a boost query as an optional clause to the main query?
      final boolean hasOptBoost = needsScores && ((boostQueries != null && !boostQueries.isEmpty())
            || (boostFunctions != null && !boostFunctions.isEmpty())
            || (phraseFieldQuery != null)
            || hasMultiplicativeBoosts
            || (hasQuerqyBoostQueries && !useReRankForBoostQueries));

      if (hasOptBoost) {

          final BooleanQuery.Builder builder = new BooleanQuery.Builder();

          if (mainQuery instanceof MatchAllDocsQuery) {
              builder.add(mainQuery, Occur.FILTER);
          } else {
              builder.add(LuceneQueryUtil.boost(mainQuery, userQueryWeight), Occur.MUST);
          }

          if (boostQueries != null) {
              for (final Query f : boostQueries) {
                builder.add(f, BooleanClause.Occur.SHOULD);
              }
          }

          if (boostFunctions != null) {
              for (final Query f : boostFunctions) {
                builder.add(f, BooleanClause.Occur.SHOULD);
              }
          }

          if (phraseFieldQuery != null) {
              builder.add(phraseFieldQuery, BooleanClause.Occur.SHOULD);
          }

          if (hasQuerqyBoostQueries && !useReRankForBoostQueries) {
              for (final Query q : querqyBoostQueries) {
                  builder.add(q, BooleanClause.Occur.SHOULD);
              }
          }

          final BooleanQuery bq = builder.build();

          if (hasMultiplicativeBoosts) {
              if (multiplicativeBoosts.size() > 1) {
                  ValueSource prod = new ProductFloatFunction(
                          multiplicativeBoosts.toArray(new ValueSource[multiplicativeBoosts.size()]));
                  mainQuery = new BoostedQuery(bq, prod);
              } else {
                  mainQuery = new BoostedQuery(bq, multiplicativeBoosts.get(0));
              }
          } else {
              mainQuery = bq;
          }
      }

      if (useReRankForBoostQueries && hasQuerqyBoostQueries) {

          final BooleanQuery.Builder builder = new BooleanQuery.Builder();

          for (final Query q : querqyBoostQueries) {
              builder.add(q, BooleanClause.Occur.SHOULD);
          }

          mainQuery = new QuerqyReRankQuery(mainQuery, builder.build(), reRankNumDocs, 1.0);
      }

      return mainQuery;

   }

    @Override
    public Query getHighlightQuery() throws SyntaxError {
        if (userQuery == null) {
            parse();
        }
        return userQuery;
    }

    protected List<Query> getQuerqyBoostQueries(final ExpandedQuery expandedQuery) throws SyntaxError {

        final List<Query> result = transformBoostQueries(expandedQuery.getBoostUpQueries(), boostQueryWeight);
        final List<Query> down = transformBoostQueries(expandedQuery.getBoostDownQueries(), -negativeBoostWeight);

        if (down != null) {
            if (result == null) {
                return down;
            } else {
                result.addAll(down);
            }
        }

        return result;

    }


    public List<Query> transformBoostQueries(final Collection<BoostQuery> boostQueries, final float factor)
            throws SyntaxError {

        final List<Query> result;

        if (boostQueries != null && !boostQueries.isEmpty()) {

            result = new LinkedList<>();

            for (BoostQuery bq : boostQueries) {

                Query luceneQuery = null;
                QuerqyQuery<?> boostQuery = bq.getQuery();

                if (boostQuery instanceof RawQuery) {

                    QParser bqp = QParser.getParser(((RawQuery) boostQuery).getQueryString(), null, req);
                    luceneQuery = bqp.getQuery();

                } else if (boostQuery instanceof querqy.model.Query) {

                    LuceneQueryBuilder luceneQueryBuilder =
                            new LuceneQueryBuilder(boostTermQueryBuilder, queryAnalyzer,
                                    boostSearchFieldsAndBoostings,
                                    config.getTieBreaker(), termQueryCache);
                    try {

                        luceneQuery = luceneQueryBuilder.createQuery((querqy.model.Query) boostQuery, factor < 0f);

                        if (luceneQuery != null) {
                            luceneQuery = wrapQuery(luceneQuery);
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }

                if (luceneQuery != null) {
                    final float boost = bq.getBoost() * factor;
                    if (boost != 1f) {
                        result.add(new org.apache.lucene.search.BoostQuery(luceneQuery, boost));
                    } else {
                        result.add(luceneQuery);

                    }

                }

            }

        } else {
            result = null;
        }

        return result;
    }

    public ExpandedQuery makeExpandedQuery() {

        return new ExpandedQuery(querqyParser.parse(qstr));

    }


    public Query makePhraseFieldQueries(final QuerqyQuery<?> userQuery) {


        if (userQuery instanceof querqy.model.Query) {

            final List<querqy.model.BooleanClause> clauses = ((querqy.model.Query) userQuery).getClauses();

            if (clauses.size() > 1) {

                final List<FieldParams> allPhraseFields = new LinkedList<>();
                final IndexSchema schema = req.getSchema();

                for (final FieldParams field : config.getAllPhraseFields()) {
                    if (isFieldPhraseQueryable(schema.getFieldOrNull(field.getField()))) {
                        allPhraseFields.add(field);
                    }
                }



                if (!allPhraseFields.isEmpty()) {

                    final List<String> sequence = new LinkedList<>();

                    for (final querqy.model.BooleanClause clause : clauses) {

                        if (clause instanceof DisjunctionMaxQuery) {

                            final DisjunctionMaxQuery dmq = (DisjunctionMaxQuery) clause;

                            if (dmq.occur != querqy.model.SubQuery.Occur.MUST_NOT) {

                                for (final DisjunctionMaxClause dmqClause : dmq.getClauses()) {
                                    if (dmqClause instanceof Term) {

                                        final ComparableCharSequence value = ((Term) dmqClause).getValue();
                                        final int length = value.length();
                                        final StringBuilder sb = new StringBuilder(length);
                                        for (int i = 0; i < length; i++) {
                                            sb.append(value.charAt(i));
                                        }
                                        sequence.add(sb.toString());
                                        break;
                                    }
                                }

                            }
                        }

                    }

                    if (sequence.size() > 1) {

                        final List<Query> disjuncts = new LinkedList<>();

                        final QueryBuilder queryBuilder = new QueryBuilder(schema.getQueryAnalyzer());

                        final List<String>[] shingles = new List[4];
                        String pf = null;


                        for (final FieldParams fieldParams : allPhraseFields) {

                            final int n = fieldParams.getWordGrams();
                            final int slop = fieldParams.getSlop();
                            final String fieldname = fieldParams.getField();

                            if (n == 0) {

                                if (pf == null) {
                                    final StringBuilder sb = new StringBuilder(getString().length());
                                    for (final String term : sequence) {
                                        if (sb.length() > 0) {
                                            sb.append(' ');
                                        }
                                        sb.append(term);
                                    }
                                    pf = sb.toString();
                                }
                                final Query pq = queryBuilder.createPhraseQuery(fieldname, pf, slop);
                                if (pq != null) {
                                    disjuncts.add(LuceneQueryUtil.boost(pq, fieldParams.getBoost()));
                                }

                            } else if (n <= sequence.size()) {

                                if (shingles[n] == null) {
                                    shingles[n] = new LinkedList<>();
                                    for (int i = 0, lenI = sequence.size() - n + 1; i < lenI; i++) {
                                        final StringBuilder sb = new StringBuilder();

                                        for (int j = i, lenJ = j + n; j < lenJ; j++) {
                                            if (sb.length() > 0) {
                                                sb.append(' ');
                                            }
                                            sb.append(sequence.get(j));
                                        }
                                        shingles[n].add(sb.toString());
                                    }
                                }


                                final List<Query> nGramQueries = new ArrayList<>(shingles[n].size());

                                for (final String nGram : shingles[n]) {
                                    final Query pq = queryBuilder.createPhraseQuery(fieldname, nGram, slop);
                                    if (pq != null) {
                                        nGramQueries.add(pq);
                                    }
                                }

                                switch (nGramQueries.size()) {
                                    case 0: break;
                                    case 1: {

                                        final Query nGramQuery = nGramQueries.get(0);
                                        disjuncts.add(LuceneQueryUtil.boost(nGramQuery, fieldParams.getBoost()));
                                        break;

                                    }
                                    default:

                                        final BooleanQuery.Builder builder = new BooleanQuery.Builder();

                                        for (final Query nGramQuery : nGramQueries) {
                                            builder.add(nGramQuery, Occur.SHOULD);
                                        }

                                        final BooleanQuery bq = builder.build();
                                        disjuncts.add(LuceneQueryUtil.boost(bq, fieldParams.getBoost()));
                                }
                            }
                        }

                        switch (disjuncts.size()) {
                            case 0: break;
                            case 1: return disjuncts.get(0);
                            default :
                                return new org.apache.lucene.search.DisjunctionMaxQuery(disjuncts, qpfTie);
                        }

                    }


                }
            }
        }

        return null;

    }

    public boolean isFieldPhraseQueryable(final SchemaField field) {
        if (field != null) {
            final FieldType fieldType = field.getType();
            if ((fieldType instanceof TextField) && !field.omitPositions() && !field.omitTermFreqAndPositions()) {
                return true;
            }
        }

        return false;
    }

   /**
    * @param query
    */
   public Query applyMinShouldMatch(final Query query) {

       if (!(query instanceof BooleanQuery)) {
           return query;
       }

       final BooleanQuery bq = (BooleanQuery) query;
       final List<BooleanClause> clauses = bq.clauses();
       if (clauses.size() < 2) {
           return bq;
       }

       for (final BooleanClause clause : clauses) {
           if ((clause.getQuery() instanceof BooleanQuery) && (clause.getOccur() != Occur.MUST)) {
               return bq; // seems to be a complex query with sub queries - do not
               // apply mm
           }
       }

       return SolrPluginUtils.setMinShouldMatch(bq, config.getMinShouldMatch());

   }


    public void applyFilterQueries(final ExpandedQuery expandedQuery) throws SyntaxError {

      final Collection<QuerqyQuery<?>> filterQueries = expandedQuery.getFilterQueries();

      if (filterQueries != null && !filterQueries.isEmpty()) {
          
          final List<Query> fqs = new LinkedList<>();
          
          for (final QuerqyQuery<?> qfq : filterQueries) {
          
              if (qfq instanceof RawQuery) {
                  
                  final QParser fqParser = QParser.getParser(((RawQuery) qfq).getQueryString(), null, req);
                  fqs.add(fqParser.getQuery());
                  
              } else {
                  
                  builder.reset();
                  
                  try {
                      fqs.add(builder.createQuery(qfq));
                  } catch (final IOException e) {
                        throw new RuntimeException(e);
                  }
                  
              }
          }

          this.filterQueries = fqs;
      }
         
   }

   public Query makeUserQuery(final ExpandedQuery expandedQuery) {

      builder.reset();

      try {

          final Query query = wrapQuery(applyMinShouldMatch(builder.createQuery(expandedQuery.getUserQuery())));

          userQuery = (needsScores || query instanceof MatchAllDocsQuery) ? query : new ConstantScoreQuery(query);

          return userQuery;

      } catch (final IOException e) {
         throw new RuntimeException(e);
      }
   }

    public Query wrapQuery(final Query query) {

        if (query == null || dfc == null) {

            return query;

        } else {

            final WrappedQuery wrappedQuery = new WrappedQuery(query);
            wrappedQuery.setCache(true);
            wrappedQuery.setCacheSep(false);
            return wrappedQuery;

        }

    }
   
   public List<Query> getFilterQueries() {
       return filterQueries;
   }

   public Map<String, Object> getContext() {
       return context;
   }

    @Override
    public void addDebugInfo(final NamedList<Object> debugInfo) {

        super.addDebugInfo(debugInfo);

        debugInfo.add("querqy.parser", querqyParser.getClass().getName());

        if (context != null) {

            @SuppressWarnings("unchecked") final List<String> rulesDebugInfo =
                    (List<String>) context.get(ContextAwareQueryRewriter.CONTEXT_KEY_DEBUG_DATA);

            if (rulesDebugInfo != null) {
                debugInfo.add("querqy.rewrite", rulesDebugInfo);
            }

        }

    }

    /**
    * Copied from DisMaxQParser
    * 
    * @param solrParams
    * @return
    * @throws SyntaxError
    */
   public Query getAlternateUserQuery(final SolrParams solrParams) throws SyntaxError {
      String altQ = solrParams.get(DisMaxParams.ALTQ);
      if (altQ != null) {
         QParser altQParser = subQuery(altQ, null);
         return altQParser.getQuery();
      } else {
         return null;
      }
   }

   /**
    * Given a string containing fieldNames and boost info, converts it to a Map
    * from field name to boost info.
    *
    * <p>
    * Doesn't care if boost info is negative, you're on your own.
    * </p>
    * <p>
    * Doesn't care if boost info is missing, again: you're on your own.
    * </p>
    *
    * @param in
    *           a String like "fieldOne^2.3 fieldTwo fieldThree^-0.4"
    * @return Map of fieldOne =&gt; 2.3, fieldTwo =&gt; null, fieldThree =&gt;
    *         -0.4
    */
   public static Map<String, Float> parseFieldBoosts(final String in, final Float defaultBoost) {
      return parseFieldBoosts(new String[] { in }, defaultBoost);
   }

   /**
    * Like <code>parseFieldBoosts(String)</code>, but parses all the strings in
    * the provided array (which may be null).
    *
    * @param fieldLists
    *           an array of Strings eg.
    *           <code>{"fieldOne^2.3", "fieldTwo", fieldThree^-0.4}</code>
    * @return Map of fieldOne =&gt; 2.3, fieldTwo =&gt; null, fieldThree =&gt;
    *         -0.4
    */
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

         final String[] bb = whitespacePattern.split(in);
         for (final String s : bb) {
            final String[] bbb = caratPattern.split(s);
            out.put(bbb[0], 1 == bbb.length ? defaultBoost : Float.valueOf(bbb[1]));
         }
      }
      return out;
   }

   /**
    * Copied from DisMxQParser (as we don't handle user fields/aliases yet)
    * 
    * Uses {@link SolrPluginUtils#parseFieldBoosts(String)} with the 'qf'
    * parameter. Falls back to the 'df' parameter.
    */
   public static Map<String, Float> parseQueryFields(final IndexSchema indexSchema, final SolrParams solrParams,
                                                     final String fieldName, final Float defaultBoost, final boolean useDfFallback)
         throws SyntaxError {
      final Map<String, Float> queryFields = parseFieldBoosts(solrParams.getParams(fieldName), defaultBoost);
      if (queryFields.isEmpty() && useDfFallback) {
         final String df = solrParams.get(CommonParams.DF);
         if (df == null) {
            throw new SyntaxError("Neither " + fieldName + ", " + CommonParams.DF
                  + ", nor the default search field are present.");
         }
         queryFields.put(df, defaultBoost);
      }
      return queryFields;
   }

   public class PublicExtendedDismaxConfiguration extends ExtendedDismaxConfiguration {

      final float generatedFieldBoostFactor;

      /**
       * @param localParams
       * @param params
       * @param req
       */
      public PublicExtendedDismaxConfiguration(final SolrParams localParams,
            final SolrParams params, final SolrQueryRequest req) {
         super(localParams, params, req);
         generatedFieldBoostFactor = solrParams.getFloat(GFB, 1f);
      }

      public float getTieBreaker() {
         return tiebreaker;
      }

      public String getMinShouldMatch() {
         return minShouldMatch;
      }

   }

    private void updateRulesCriteria(ExpandedQuery expandedQuery) {
        String sort = params.get("rules.criteria.sort");
        String size = params.get("rules.criteria.size");
        String[] filters = params.getParams("rules.criteria.filter");

        if (sort != null) {
            String[] sortCriteria = sort.split("\\s+");
            if (sortCriteria.length == 2) {
                expandedQuery.addCrieteria(new SortCriteria(sortCriteria[0], sortCriteria[1]));
            }
        }

        if (size != null) {
            expandedQuery.addCrieteria(new SelectionCriteria(Integer.valueOf(size)));
        }

        if (filters != null) {
            Arrays.asList(filters).parallelStream().forEach(filterStr -> {
                String[] filterArr = filterStr.split(":");
                if (filterArr.length == 2) {
                    expandedQuery
                            .addCrieteria(new FilterCriteria(filterArr[0].trim(), filterArr[1].trim()));
                }
            });
        }
    }

    public Criterion getQuerqyCriterion() {
        if (expandedQuery == null || CollectionUtils.isEmpty(expandedQuery.getCriterion())) {
            return new Criterion();
        }
        return expandedQuery.getCriterion();
    }

    public String getQuerqyAppliedRules() {
        if (expandedQuery == null) {
            return "";
        }
        return expandedQuery.getAppliedRuleIds();
    }



}
