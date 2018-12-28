package querqy.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queries.function.BoostedQuery;
import org.apache.lucene.queries.function.valuesource.ProductFloatFunction;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.QueryBuilder;
import org.apache.lucene.queries.function.ValueSource;

import querqy.ComparableCharSequence;
import querqy.lucene.SearchEngineRequestAdapter.SyntaxException;
import querqy.lucene.rewrite.DocumentFrequencyCorrection;
import querqy.lucene.rewrite.LuceneQueryBuilder;
import querqy.lucene.rewrite.LuceneTermQueryBuilder;
import querqy.lucene.rewrite.SearchFieldsAndBoosting;
import querqy.lucene.rewrite.SearchFieldsAndBoosting.FieldBoostModel;
import querqy.lucene.rewrite.TermQueryBuilder;
import querqy.model.BoostQuery;
import querqy.model.DisjunctionMaxClause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.QuerqyQuery;
import querqy.model.RawQuery;
import querqy.model.Term;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.ContextAwareQueryRewriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by rene on 23/05/2017.
 */
public class QueryParsingController {

    protected static final float DEFAULT_TIEBREAKER = 0f;
    protected static final float DEFAULT_PHRASE_BOOST_TIEBREAKER = 0f;

    protected final SearchEngineRequestAdapter requestAdapter;
    protected final String queryString;
    protected final boolean needsScores;
    protected final Analyzer queryAnalyzer;
    protected final SearchFieldsAndBoosting searchFieldsAndBoosting;
    protected final DocumentFrequencyCorrection dfc;
    protected final boolean debugQuery;
    protected final LuceneQueryBuilder builder;
    protected final TermQueryBuilder boostTermQueryBuilder;
    protected final SearchFieldsAndBoosting boostSearchFieldsAndBoostings;
    protected final boolean useReRankForBoostQueries;

    public QueryParsingController(final SearchEngineRequestAdapter requestAdapter) {
        this.requestAdapter = requestAdapter;
        this.queryString = getValidatedQueryString();
        needsScores = requestAdapter.needsScores();
        queryAnalyzer = requestAdapter.getQueryAnalyzer();

        final Map<String, Float> queryFieldsAndBoostings = requestAdapter.getQueryFieldsAndBoostings();
        final float gfb = requestAdapter.getGeneratedFieldBoost().orElse(1f);
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
                needsScores ? requestAdapter.getFieldBoostModel() : FieldBoostModel.FIXED,
                queryFieldsAndBoostings,
                generatedQueryFieldsAndBoostings,
                gfb);

        if (!needsScores) {
            useReRankForBoostQueries = false;
            dfc = null;
            boostTermQueryBuilder = null;
            boostSearchFieldsAndBoostings = null;
            builder = new LuceneQueryBuilder(new LuceneTermQueryBuilder(), queryAnalyzer, searchFieldsAndBoosting, 1f,
                    requestAdapter.getTermQueryCache());
        } else {
            useReRankForBoostQueries = requestAdapter.useReRankQueryForBoosting();

            final QuerySimilarityScoring userQuerySimilarityScoring = requestAdapter.getUserQuerySimilarityScoring();
            final TermQueryBuilder userTermQueryBuilder = userQuerySimilarityScoring.createTermQueryBuilder(null);
            dfc = userTermQueryBuilder.getDocumentFrequencyCorrection().orElse(null);

            final QuerySimilarityScoring boostQuerySimilarityScoring = requestAdapter.getBoostQuerySimilarityScoring();
            boostTermQueryBuilder = boostQuerySimilarityScoring.createTermQueryBuilder(dfc);

            boostSearchFieldsAndBoostings = requestAdapter.useFieldBoostingInQuerqyBoostQueries()
                    ? searchFieldsAndBoosting
                    : searchFieldsAndBoosting.withFieldBoostModel(FieldBoostModel.NONE);




            builder = new LuceneQueryBuilder(userTermQueryBuilder,
                    queryAnalyzer, searchFieldsAndBoosting, requestAdapter.getTiebreaker().orElse(DEFAULT_TIEBREAKER),
                    requestAdapter.getTermQueryCache());

        }


        debugQuery = requestAdapter.isDebugQuery();


    }



    public LuceneQueries process() throws SyntaxException {


        Query mainQuery;


        ExpandedQuery expandedQuery = null;

        final List<Query> querqyBoostQueries;
        final Query phraseFieldQuery;
        final List<Query> filterQueries;

        if (requestAdapter.isMatchAllQuery(queryString)) {

            mainQuery = new MatchAllDocsQuery();
            if (dfc != null) dfc.finishedUserQuery();
            phraseFieldQuery = null;
            querqyBoostQueries = Collections.emptyList();
            filterQueries = Collections.emptyList();

        } else {

            expandedQuery = new ExpandedQuery(requestAdapter.createQuerqyParser().orElseGet(WhiteSpaceQuerqyParser::new)
                    .parse(queryString));

            phraseFieldQuery = needsScores ? makePhraseFieldQueries(expandedQuery.getUserQuery()) : null;


            final Map<String, Object> context = requestAdapter.getContext();
            if (debugQuery) {
                context.put(ContextAwareQueryRewriter.CONTEXT_KEY_DEBUG_ENABLED, true);
            }
            expandedQuery = requestAdapter.getRewriteChain().rewrite(expandedQuery, context);

            mainQuery = makeUserQuery(expandedQuery, builder);

            if (dfc != null) dfc.finishedUserQuery();


            filterQueries = applyFilterQueries(expandedQuery);
            querqyBoostQueries = needsScores ? getQuerqyBoostQueries(expandedQuery) : Collections.emptyList();

        }

        final Query userQuery = mainQuery;
        final List<Query> additiveBoosts;
        final List<Query> multiplicativeBoosts;
        if (needsScores) {
            additiveBoosts = requestAdapter.getAdditiveBoosts();
            multiplicativeBoosts = requestAdapter.getMultiplicativeBoosts();
        } else {
            additiveBoosts = multiplicativeBoosts = Collections.emptyList();
        }

        final boolean hasMultiplicativeBoosts = multiplicativeBoosts != null && !multiplicativeBoosts.isEmpty();
        final boolean hasQuerqyBoostQueries = !querqyBoostQueries.isEmpty();

        // do we have to add a boost query as an optional clause to the main query?
        final boolean hasOptBoost = needsScores && ((additiveBoosts != null && !additiveBoosts.isEmpty())
                || (phraseFieldQuery != null)
                || hasMultiplicativeBoosts
                || (hasQuerqyBoostQueries && !useReRankForBoostQueries));

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
                    final ValueSource prod = new ProductFloatFunction(
                            (ValueSource[]) multiplicativeBoosts
                                    .stream()
                                    .map(LuceneQueryUtil::queryToValueSource)
                                    .toArray());
                    mainQuery = new BoostedQuery(bq, prod);
                } else {
                    mainQuery = new BoostedQuery(bq, LuceneQueryUtil.queryToValueSource(multiplicativeBoosts.get(0)));
                }
            } else {
                mainQuery = bq;
            }
        }

        return (useReRankForBoostQueries && hasQuerqyBoostQueries)
                ? new LuceneQueries(mainQuery, filterQueries, querqyBoostQueries, userQuery, dfc != null)
                : new LuceneQueries(mainQuery, filterQueries, userQuery, dfc != null);


    }

    public List<Query> applyFilterQueries(final ExpandedQuery expandedQuery) throws SyntaxException {

        final Collection<QuerqyQuery<?>> filterQueries = expandedQuery.getFilterQueries();

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


    protected Query makePhraseFieldQueries(final QuerqyQuery<?> userQuery) {

        if (userQuery instanceof querqy.model.Query) {

            final List<querqy.model.BooleanClause> clauses = ((querqy.model.Query) userQuery).getClauses();

            if (clauses.size() > 1) {

                final List<PhraseBoostFieldParams> allPhraseFields = requestAdapter.getQueryablePhraseBoostFieldParams();

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

                        final QueryBuilder queryBuilder = new QueryBuilder(queryAnalyzer);

                        final List<String>[] shingles = new List[4];
                        String pf = null;


                        for (final PhraseBoostFieldParams fieldParams : allPhraseFields) {

                            final int n = fieldParams.getWordGrams();
                            final int slop = fieldParams.getSlop();
                            final String fieldname = fieldParams.getField();

                            if (n == 0) {

                                if (pf == null) {
                                    final StringBuilder sb = new StringBuilder(queryString.length());
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
                                            builder.add(nGramQuery, BooleanClause.Occur.SHOULD);
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
                                return new org.apache.lucene.search.DisjunctionMaxQuery(disjuncts,
                                        requestAdapter.getPhraseBoostTiebreaker()
                                                .orElseGet(() -> requestAdapter.getTiebreaker()
                                                        .orElse(DEFAULT_TIEBREAKER)));
                        }

                    }


                }
            }
        }

        return null;

    }

    public Query makeUserQuery(final ExpandedQuery expandedQuery, LuceneQueryBuilder builder) {

        builder.reset();
        // FIXME wrap
        final Query query = requestAdapter.applyMinimumShouldMatch(builder.createQuery(expandedQuery.getUserQuery()));

        return (needsScores || query instanceof MatchAllDocsQuery) ? query : new ConstantScoreQuery(query);

    }

    protected List<Query> getQuerqyBoostQueries(final ExpandedQuery expandedQuery) throws SyntaxException {

        final List<Query> result = transformBoostQueries(expandedQuery.getBoostUpQueries(),
                requestAdapter.getBoostQueryWeight().orElse(1f));
        final List<Query> down = transformBoostQueries(expandedQuery.getBoostDownQueries(),
                -requestAdapter.getNegativeBoostQueryWeight().map(Math::abs).orElse(1f));

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
                                    requestAdapter.getTermQueryCache());

                    luceneQuery = luceneQueryBuilder.createQuery((querqy.model.Query) boostQuery, factor < 0f);

                    // TODO
//                    if (luceneQuery != null) {
//                        luceneQuery = wrapQuery(luceneQuery);
//                    }


                } else {
                    luceneQuery = null;
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




}
