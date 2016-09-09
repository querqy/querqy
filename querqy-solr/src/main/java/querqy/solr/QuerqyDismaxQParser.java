/**
 * 
 */
package querqy.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queries.function.BoostedQuery;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.valuesource.ProductFloatFunction;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.TextField;
import org.apache.solr.search.ExtendedDismaxQParser;
import org.apache.solr.search.FieldParams;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.search.WrappedQuery;

import org.apache.solr.util.SolrPluginUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import querqy.lucene.rewrite.DocumentFrequencyCorrection;
import querqy.lucene.rewrite.LuceneQueryBuilder;
import querqy.lucene.rewrite.SearchFieldsAndBoosting;
import querqy.lucene.rewrite.SearchFieldsAndBoosting.FieldBoostModel;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.model.BoostQuery;
import querqy.model.DisjunctionMaxClause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.QuerqyQuery;
import querqy.model.RawQuery;
import querqy.model.Term;
import querqy.parser.QuerqyParser;
import querqy.rewrite.RewriteChain;

/**
 * @author rene
 *
 */
public class QuerqyDismaxQParser extends ExtendedDismaxQParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuerqyDismaxQParser.class);

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
    public static final String BM                       = "bm";

    /**
     *  Integrate Querqy boost queries with the main query as a re-rank query
     */
    public static final String BM_RERANK                = "rerank";

    /**
     * The number of docs in the main query result to use for re-ranking when bm=rerank
     */
    public static final String BM_RERANK_NUMDOCS        = "bm.rerank.numDocs";

    /**
     * The default value for {@link #BM_RERANK_NUMDOCS}
     */
    public static final int DEFAULT_RERANK_NUMDOCS      = 500;

    /**
     * add Querqy boost queries to the main query as an optional boolean clause
     */
    public static final String BM_OPT                   = "opt";

    /**
     * The default boost method (= {@link #BM})
     */
    public static final String BM_DEFAULT              = BM_OPT;



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
    final DocumentFrequencyCorrection dfc;

    protected List<Query> filterQueries = null;
    protected Map<String, Object> context = null;

    protected PublicExtendedDismaxConfiguration config = null;
    protected List<Query> boostQueries = null;

    protected final boolean useReRankForBoostQueries;
    protected final int reRankNumDocs;
    protected final TermQueryCache termQueryCache;

    public QuerqyDismaxQParser(String qstr, SolrParams localParams, SolrParams params,
         SolrQueryRequest req, RewriteChain rewriteChain, QuerqyParser querqyParser, TermQueryCache termQueryCache)
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
        generatedQueryFields = parseQueryFields(req.getSchema(), solrParams, GQF, null, false);
        if (generatedQueryFields.isEmpty()) {
            for (Map.Entry<String, Float> entry: userQueryFields.entrySet()) {
                generatedQueryFields.put(entry.getKey(), entry.getValue() * config.generatedFieldBoostFactor);
            }
        } else {
            for (Map.Entry<String, Float> entry: generatedQueryFields.entrySet()) {
                if (entry.getValue() == null) {
                    String name = entry.getKey();
                    Float nonGeneratedBoostFactor = userQueryFields.get(name);
                    if (nonGeneratedBoostFactor == null) {
                        nonGeneratedBoostFactor = 1f;
                    }
                    entry.setValue(nonGeneratedBoostFactor * config.generatedFieldBoostFactor);
                }
            }
        }
        dfc = new DocumentFrequencyCorrection();

        useReRankForBoostQueries = BM_RERANK.equals(solrParams.get(BM, BM_DEFAULT));
        if (useReRankForBoostQueries) {
            reRankNumDocs = solrParams.getInt(BM_RERANK_NUMDOCS, DEFAULT_RERANK_NUMDOCS);
        } else {
            reRankNumDocs = 0;
        }

     
        SearchFieldsAndBoosting searchFieldsAndBoosting =
              new SearchFieldsAndBoosting(getFieldBoostModelFromParam(solrParams), 
                      userQueryFields, generatedQueryFields, config.generatedFieldBoostFactor);
      
        builder = new LuceneQueryBuilder(dfc, queryAnalyzer, searchFieldsAndBoosting, config.getTieBreaker(), termQueryCache);
      
    }
   
   protected FieldBoostModel getFieldBoostModelFromParam(SolrParams solrParams) {
       String fbm = solrParams.get(FBM, FBM_DEFAULT);
       if (fbm.equals(FBM_FIXED)) {
           return FieldBoostModel.FIXED;
       } else if (fbm.equals(FBM_PRMS)) {
           return FieldBoostModel.PRMS;
       }
       
       throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, 
               "Unknown field boost model: " + fbm);
   }

   @Override
   protected PublicExtendedDismaxConfiguration createConfiguration(String qstr,
         SolrParams localParams, SolrParams params, SolrQueryRequest req) {
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
      String userQuery = getString();
      if (userQuery == null) {
         throw new SyntaxError("query string is null");
      }
      userQuery = userQuery.trim();
      if (userQuery.length() == 0) {
         throw new SyntaxError("query string is empty");
      }

      Query mainQuery;
      ExpandedQuery expandedQuery = null;
      List<Query> querqyBoostQueries = null;

      if ((userQuery.charAt(0) == '*') && (userQuery.length() == 1 || MATCH_ALL.equals(userQuery))) {
         mainQuery = new MatchAllDocsQuery();
         dfc.finishedUserQuery();
      } else {
         expandedQuery = makeExpandedQuery();
         
         context = new HashMap<>();
         expandedQuery = rewriteChain.rewrite(expandedQuery, context);
         
         mainQuery = makeMainQuery(expandedQuery);
         
         dfc.finishedUserQuery();
         
         applyFilterQueries(expandedQuery);
         querqyBoostQueries = getQuerqyBoostQueries(expandedQuery);

      }

      boostQueries = getBoostQueries();
      List<Query> boostFunctions = getBoostFunctions();
      List<ValueSource> multiplicativeBoosts = getMultiplicativeBoosts();
      List<Query> phraseFieldQueries = getPhraseFieldQueries(expandedQuery);

      boolean hasMultiplicativeBoosts = multiplicativeBoosts != null && !multiplicativeBoosts.isEmpty();
      boolean hasQuerqyBoostQueries = querqyBoostQueries != null && !querqyBoostQueries.isEmpty();

       // do we have to add a boost query as an optional clause to the main query?
      boolean hasOptBoost = (boostQueries != null && !boostQueries.isEmpty())
            || (boostFunctions != null && !boostFunctions.isEmpty())
            || !phraseFieldQueries.isEmpty()
            || hasMultiplicativeBoosts
            || (hasQuerqyBoostQueries && !useReRankForBoostQueries);

      if (hasOptBoost) {

         BooleanQuery bq = new BooleanQuery(true);
         bq.add(mainQuery, Occur.MUST);

         if (boostQueries != null) {
            for (Query f : boostQueries) {
               bq.add(f, BooleanClause.Occur.SHOULD);
            }
         }

         if (boostFunctions != null) {
            for (Query f : boostFunctions) {
               bq.add(f, BooleanClause.Occur.SHOULD);
            }
         }

         if (phraseFieldQueries != null) {
            for (Query pf : phraseFieldQueries) {
               bq.add(pf, BooleanClause.Occur.SHOULD);
            }
         }

         if (hasQuerqyBoostQueries && !useReRankForBoostQueries) {
            for (Query q : querqyBoostQueries) {
               bq.add(q, BooleanClause.Occur.SHOULD);
            }
         }

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

          final BooleanQuery boostBq = new BooleanQuery(true);

          for (final Query q : querqyBoostQueries) {
              boostBq.add(q, BooleanClause.Occur.SHOULD);
          }

          mainQuery = new QuerqyReRankQuery(mainQuery, boostBq, reRankNumDocs, 1.0);
      }

      return mainQuery;

   }

   protected List<Query> getQuerqyBoostQueries(ExpandedQuery expandedQuery) throws SyntaxError {

      List<Query> result = transformBoostQueries(expandedQuery.getBoostUpQueries(), 1f);
      List<Query> down = transformBoostQueries(expandedQuery.getBoostDownQueries(), -1f);

      if (down != null) {
         if (result == null) {
            result = down;
         } else {
            result.addAll(down);
         }
      }

      return result;
   }

    public List<Query> transformBoostQueries(Collection<BoostQuery> boostQueries, float factor) throws SyntaxError {

        List<Query> result = null;

        if (boostQueries != null && !boostQueries.isEmpty()) {



            result = new LinkedList<>();

            for (BoostQuery bq : boostQueries) {

                Query luceneQuery = null;
                QuerqyQuery<?> boostQuery = bq.getQuery();

                if (boostQuery instanceof RawQuery) {

                    QParser bqp = QParser.getParser(((RawQuery) boostQuery).getQueryString(), null, req);
                    luceneQuery = bqp.getQuery();
                    luceneQuery.setBoost(bq.getBoost() * factor);

                } else if (boostQuery instanceof querqy.model.Query) {

                    LuceneQueryBuilder luceneQueryBuilder =
                            new LuceneQueryBuilder(dfc, queryAnalyzer,
                                    builder.getSearchFieldsAndBoosting().multiply(factor * bq.getBoost()),
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
                    result.add(luceneQuery);
                }

            }

        }

        return result;
    }

    public ExpandedQuery makeExpandedQuery() {

        return new ExpandedQuery(querqyParser.parse(qstr));

    }

   public List<Query> getPhraseFieldQueries(ExpandedQuery querqyQuery) {

      List<Query> result = new ArrayList<>();

      if (querqyQuery != null) {

         List<querqy.model.BooleanClause> clauses = querqyQuery.getUserQuery().getClauses();

         if (clauses.size() > 1) {

            List<FieldParams> allPhraseFields = config.getAllPhraseFields();

            if (allPhraseFields != null && !allPhraseFields.isEmpty()) {

               List<Term> sequence = new LinkedList<>();

               for (querqy.model.BooleanClause clause : clauses) {

                  if ((!clause.isGenerated()) && (clause instanceof DisjunctionMaxQuery)) {

                     DisjunctionMaxQuery dmq = (DisjunctionMaxQuery) clause;

                     if (dmq.occur != querqy.model.SubQuery.Occur.MUST_NOT) {

                        for (DisjunctionMaxClause dmqClause : dmq.getClauses()) {
                           if (!(dmqClause.isGenerated()) && (dmqClause instanceof Term)) {
                              sequence.add((Term) dmqClause);
                              break;
                           }
                        }

                     }
                  }

               }

               if (sequence.size() > 1) {

                  IndexSchema schema = req.getSchema();

                  for (FieldParams fieldParams : allPhraseFields) {

                     if (isFieldPhraseQueryable(schema.getFieldOrNull(fieldParams.getField()))) {

                        int n = fieldParams.getWordGrams();
                        String fieldname = fieldParams.getField();

                        if (n == 0) {

                           PhraseQuery pq = new PhraseQuery();
                           pq.setSlop(fieldParams.getSlop());
                           pq.setBoost(fieldParams.getBoost());

                           for (Term term : sequence) {
                              pq.add(
                                    new org.apache.lucene.index.Term(fieldname,
                                          new BytesRef(term))
                                    );
                           }

                           result.add(pq);

                        } else {
                           for (int i = 0, lenI = sequence.size() - n + 1; i < lenI; i++) {
                              PhraseQuery pq = new PhraseQuery();
                              pq.setSlop(fieldParams.getSlop());
                              pq.setBoost(fieldParams.getBoost());
                              for (int j = i, lenJ = j + n; j < lenJ; j++) {
                                 pq.add(
                                       new org.apache.lucene.index.Term(fieldname,
                                             new BytesRef(sequence.get(j)))
                                       );
                              }
                              result.add(pq);
                           }
                        }

                     }

                  }
               }

            }

         }

      }
      return result;
   }

   public boolean isFieldPhraseQueryable(SchemaField field) {
      if (field != null) {
         FieldType fieldType = field.getType();
         if ((fieldType instanceof TextField) && !field.omitPositions() && !field.omitTermFreqAndPositions()) {
            return true;
         }
      }

      return false;

   }

   /**
    * @param query
    */
   public void applyMinShouldMatch(Query query) {

      if (!(query instanceof BooleanQuery)) {
         return;
      }

      BooleanQuery bq = (BooleanQuery) query;
      BooleanClause[] clauses = bq.getClauses();
      if (clauses.length < 2) {
         return;
      }

      for (BooleanClause clause : clauses) {
         if ((clause.getQuery() instanceof BooleanQuery) && (clause.getOccur() != Occur.MUST)) {
            return; // seems to be a complex query with sub queries - do not
                    // apply mm
         }
      }

      SolrPluginUtils.setMinShouldMatch(bq, config.getMinShouldMatch());

   }

   public void applyFilterQueries(ExpandedQuery expandedQuery) throws SyntaxError {

      Collection<QuerqyQuery<?>> filterQueries = expandedQuery.getFilterQueries();

      if (filterQueries != null && !filterQueries.isEmpty()) {
          
          List<Query> fqs = new LinkedList<Query>();
          
          for (QuerqyQuery<?> qfq : filterQueries) {
          
              if (qfq instanceof RawQuery) {
                  
                  QParser fqParser = QParser.getParser(((RawQuery) qfq).getQueryString(), null, req);
                  fqs.add(fqParser.getQuery());
                  
              } else if (qfq instanceof querqy.model.Query) {
                  
                  builder.reset();
                  
                  try {
                      fqs.add(builder.createQuery((querqy.model.Query) qfq));
                  } catch (IOException e) {
                        throw new RuntimeException(e);
                  }
                  
              }
          }

          this.filterQueries = fqs;
      }
         
   }

   public Query makeMainQuery(ExpandedQuery expandedQuery) {

      builder.reset();

      try {

          Query query = builder.createQuery(expandedQuery.getUserQuery());
          applyMinShouldMatch(query);

          return wrapQuery(query);

      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

    public Query wrapQuery(Query query) {

        if (query == null || dfc == null) {

            return query;

        } else {

            WrappedQuery wrappedQuery = new WrappedQuery(query);
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

   /**
    * Copied from DisMaxQParser
    * 
    * @param solrParams
    * @return
    * @throws SyntaxError
    */
   public Query getAlternateUserQuery(SolrParams solrParams) throws SyntaxError {
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
   public static Map<String, Float> parseFieldBoosts(String in, Float defaultBoost) {
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
   public static Map<String, Float> parseFieldBoosts(String[] fieldLists, Float defaultBoost) {
      if (null == fieldLists || 0 == fieldLists.length) {
         return new HashMap<>();
      }
      Map<String, Float> out = new HashMap<>(7);
      for (String in : fieldLists) {
         if (null == in) {
            continue;
         }
         in = in.trim();
         if (in.length() == 0) {
            continue;
         }

         String[] bb = whitespacePattern.split(in);
         for (String s : bb) {
            String[] bbb = caratPattern.split(s);
            out.put(bbb[0], 1 == bbb.length ? defaultBoost : Float.valueOf(bbb[1]));
         }
      }
      return out;
   }

   /**
    * Copied from DisMxQParser (as we don't handle user fields/aliases yet)
    * 
    * Uses {@link SolrPluginUtils#parseFieldBoosts(String)} with the 'qf'
    * parameter. Falls back to the 'df' parameter or
    * {@link org.apache.solr.schema.IndexSchema#getDefaultSearchFieldName()}.
    */
   public static Map<String, Float> parseQueryFields(final IndexSchema indexSchema, final SolrParams solrParams, String fieldName, Float defaultBoost, boolean useDfFallback)
         throws SyntaxError {
      Map<String, Float> queryFields = parseFieldBoosts(solrParams.getParams(fieldName), defaultBoost);
      if (queryFields.isEmpty() && useDfFallback) {
         String df = QueryParsing.getDefaultField(indexSchema, solrParams.get(CommonParams.DF));
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
      public PublicExtendedDismaxConfiguration(SolrParams localParams,
            SolrParams params, SolrQueryRequest req) {
         super(localParams, params, req);
         generatedFieldBoostFactor = solrParams.getFloat(GFB, 1f);
      }

      public float getTieBreaker() {
         return tiebreaker;
      }

      public String getMinShouldMatch() {
         return minShouldMatch;
      }

      public float getGeneratedFieldBoostFactor() {
         return generatedFieldBoostFactor;
      }

   }


}
