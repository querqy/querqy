/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

import querqy.CompoundCharSequence;
import querqy.lucene.rewrite.BooleanQueryFactory.Clause;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.model.AbstractNodeVisitor;
import querqy.model.BooleanQuery;
import querqy.model.BoostedTerm;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.MatchAllQuery;
import querqy.model.QuerqyQuery;
import querqy.model.Term;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class LuceneQueryBuilder extends AbstractNodeVisitor<LuceneQueryFactory<?>> {

   enum ParentType {
      BQ, DMQ
   }

   final boolean normalizeBooleanQueryBoost;
   final float dmqTieBreakerMultiplier;
   final TermQueryBuilder termQueryBuilder;
   final SearchFieldsAndBoosting searchFieldsAndBoosting;
   final TermSubQueryBuilder termSubQueryBuilder;

   LinkedList<BooleanQueryFactory> clauseStack = new LinkedList<>();
   LinkedList<DisjunctionMaxQueryFactory> dmqStack = new LinkedList<>();
   boolean useBooleanQueryForDMQ = false;

   private ParentType parentType = ParentType.BQ;

   public LuceneQueryBuilder(final TermQueryBuilder termQueryBuilder, final Analyzer analyzer,
           final SearchFieldsAndBoosting searchFieldsAndBoosting,
           final float dmqTieBreakerMultiplier, final TermQueryCache termQueryCache) {
      this(   
              termQueryBuilder,
              analyzer,
              searchFieldsAndBoosting,
              dmqTieBreakerMultiplier, 
              true, 
              termQueryCache);
   }

   /**
    * <p>Field names and boost factors are applied like this:</p>
    * <p>If a term doesn't already have a field name, generate all term queries for all fields and boost factors
    * from generatedQueryFieldsAndBoostings - if the term was generated by some rewriter - or from queryFieldsAndBoostings
    * otherwise.</p>
    * <p>If a term already has a field name use the boost factor for this field from generatedQueryFieldsAndBoostings if
    * the term was generated, and from queryFieldsAndBoostings otherwise. If the respective map doesn't contain the field,
    * use the defaultGeneratedFieldBoostFactor for generated terms. If the term is not generated, treat the field name as
    * part of the term text (= "fieldname:value").</p>
    * 
    * @param termQueryBuilder The TermQueryBuilder
    * @param analyzer The query Analyzer
    * @param searchFieldsAndBoosting The search fields and their boost factors
    * @param dmqTieBreakerMultiplier The tie breaker for dismax queries
    * @param normalizeBooleanQueryBoost Iff true and if the analyzer turns a single token into multiple tokens, divide their aggregate score by their count
    * @param termQueryCache The term query cache or null
    */
    public LuceneQueryBuilder(final TermQueryBuilder termQueryBuilder, final Analyzer analyzer,
                              final SearchFieldsAndBoosting searchFieldsAndBoosting,
                              final float dmqTieBreakerMultiplier, final boolean normalizeBooleanQueryBoost,
                              final TermQueryCache termQueryCache) {
        if (termQueryBuilder == null) {
            throw new IllegalArgumentException("TermQueryBuilder must not be null");
        }

        this.searchFieldsAndBoosting = searchFieldsAndBoosting;
        this.dmqTieBreakerMultiplier = dmqTieBreakerMultiplier;
        this.normalizeBooleanQueryBoost = normalizeBooleanQueryBoost;
        this.termQueryBuilder = termQueryBuilder;
        termSubQueryBuilder = new TermSubQueryBuilder(analyzer, termQueryCache);
    }

    public void reset() {
        clauseStack.clear();
        dmqStack.clear();
        useBooleanQueryForDMQ = false;
        parentType = ParentType.BQ;
    }

    public Query createQuery(final querqy.model.Query query, final boolean useBooleanQueryForDMQ) {
        boolean tmp = this.useBooleanQueryForDMQ;
        try {
            this.useBooleanQueryForDMQ = useBooleanQueryForDMQ;
            return createQuery(query);
        } finally {
            this.useBooleanQueryForDMQ = tmp;
        }
    }
   
    public Query createQuery(final QuerqyQuery<?> query) {

        if (query instanceof querqy.model.BooleanQuery) {
            parentType = ParentType.BQ;
            LuceneQueryFactory<?> factory = query.accept(this);

            termQueryBuilder.getDocumentFrequencyCorrection()
                    .ifPresent(dfc -> factory.prepareDocumentFrequencyCorrection(dfc, false));

            return factory.createQuery(null, dmqTieBreakerMultiplier, termQueryBuilder);

        } else if (query instanceof MatchAllQuery) {

            return new MatchAllDocsQuery();

        } else {

            throw new IllegalArgumentException("Cannot handle query of type " + query.getClass().getName());

        }
    }

    @Override
    public LuceneQueryFactory<?> visit(final querqy.model.Query query) {
        return visit((BooleanQuery) query);
    }

    @Override
    public LuceneQueryFactory<?> visit(final BooleanQuery booleanQuery) {

        BooleanQueryFactory bq = new BooleanQueryFactory(normalizeBooleanQueryBoost && parentType == ParentType.DMQ);
      
        ParentType myParentType = parentType;
        parentType = ParentType.BQ;

        clauseStack.add(bq);
        super.visit(booleanQuery);
        clauseStack.removeLast();

        parentType = myParentType;

        final Clause result;

        switch (bq.getNumberOfClauses()) {
            case 0:
                // no sub-query - this can happen if analysis filters out all tokens (stopwords)
                return new NeverMatchQueryFactory();
            case 1:
                final Clause firstClause = bq.getFirstClause();
                if (firstClause.occur == Occur.SHOULD) {
                    // optimise and propagate the single clause up one level, but only
                    // if occur equals neither MUST nor MUST_NOT, which would be lost on the
                    // top level query
                    result = bq.getFirstClause();
                } else {
                    result = new Clause(bq, occur(booleanQuery.occur));
                }

                break;
            default:
                result = new Clause(bq, occur(booleanQuery.occur));
        }

        switch (parentType) {
            case BQ:
                if (!clauseStack.isEmpty()) {
                    clauseStack.getLast().add(result);
                    return bq;
                } else {// else we are the top BQ
                    return result.queryFactory;
                }
            case DMQ:
                if (result.occur != Occur.SHOULD) {
                    // create a wrapper query
                    final BooleanQueryFactory wrapper = new BooleanQueryFactory(false);
                    wrapper.add(result);
                    bq = wrapper;
                }
                dmqStack.getLast().add(bq);
                return bq;

            default:
                throw new RuntimeException("Unknown parentType " + parentType);
        }
    }

    protected Occur occur(final querqy.model.SubQuery.Occur occur) {
        switch (occur) {
            case MUST:
                return Occur.MUST;
            case MUST_NOT:
                return Occur.MUST_NOT;
            case SHOULD:
                return Occur.SHOULD;
        }
        throw new IllegalArgumentException("Cannot handle occur value: " + occur.name());
    }

    @Override
    public LuceneQueryFactory<?> visit(final DisjunctionMaxQuery disjunctionMaxQuery) {

        final ParentType myParentType = parentType;
        parentType = ParentType.DMQ;

        DisjunctionMaxQueryFactory dmq = new DisjunctionMaxQueryFactory();

        dmqStack.add(dmq);
        super.visit(disjunctionMaxQuery);
        dmqStack.removeLast();

        parentType = myParentType;

        switch (dmq.getNumberOfDisjuncts()) {
            case 0:
                // no sub-query - this can happen if analysis filters out all tokens (stopwords)
            return new NeverMatchQueryFactory();
            case 1:
                final LuceneQueryFactory<?> firstDisjunct = dmq.getFirstDisjunct();
                clauseStack.getLast().add(firstDisjunct, occur(disjunctionMaxQuery.occur));
                return firstDisjunct;
            default:
                // FIXME: we can decide this earlier --> avoid creating DMQ in case of
                // MUST_NOT
                final boolean useBQ = this.useBooleanQueryForDMQ || (disjunctionMaxQuery.occur == querqy.model.SubQuery.Occur.MUST_NOT);

                if (useBQ) {
                    // FIXME: correct to normalize boost?
                    final BooleanQueryFactory bq = new BooleanQueryFactory(false);
                    for (final LuceneQueryFactory<?> queryFactory : dmq.disjuncts) {
                        bq.add(queryFactory, Occur.SHOULD);
                    }
                    clauseStack.getLast().add(bq, occur(disjunctionMaxQuery.occur));
                    return bq;
                }

                clauseStack.getLast().add(dmq, occur(disjunctionMaxQuery.occur));
                return dmq;
        }
    }

    @Override
    public LuceneQueryFactory<?> visit(final Term term) {

        final DisjunctionMaxQueryFactory siblings = dmqStack.getLast();
      
        final String fieldname = term.getField();

        Term termToUse = null;
        try {
            FieldBoost fieldBoost = searchFieldsAndBoosting.getFieldBoost(term);
            if (fieldBoost == null) {
              
                if (fieldname != null && !term.isGenerated() && !searchFieldsAndBoosting.hasSearchField(fieldname, term)) {
                    // someone searches in a field that is not set as a search field or didn't intend to search in a field at all
                    // --> set value to fieldname + ":" + value in search in all fields
                    final Term termWithFieldInValue = new Term(null, new CompoundCharSequence(":", fieldname, term.getValue()));
                    fieldBoost = searchFieldsAndBoosting.getFieldBoost(termWithFieldInValue);
                    if (fieldBoost != null) {
                        termToUse = termWithFieldInValue;
                    }
                }
              
            } else {
                termToUse = term;
            }
            if (fieldBoost == null) {
                // TODO: move to else clause of inner if above
                throw new RuntimeException("Could not get FieldBoost for term: " + term);
            }

            // check for field boost override in BoostedTerm
            if (termToUse instanceof BoostedTerm) {
                fieldBoost = new BoostedDelegatingFieldBoost(fieldBoost, ((BoostedTerm) termToUse).getBoost());
            }
          
            for (final String searchField: searchFieldsAndBoosting.getSearchFields(termToUse)) {
                addTerm(searchField, fieldBoost, siblings, termToUse);
            }


        } catch (final IOException e) {
         // REVISIT: throw more specific exception?
         // - or save exception in Builder and then throw IOException from
         // build()
            throw new RuntimeException(e);
        }

        return null;

    }

   /**
    * 
    * <p>
    * Applies analysis to a term and adds the result to the Lucene query factory
    * tree.
    * </p>
    * 
    * <p>
    * The analysis might emit multiple tokens for the input term. If these
    * tokens constitute a sequence (according to the position attribute), a
    * BooleanQuery will be created and each position in the sequence constitutes
    * a MUST clause of this BooleanQuery. If multiple tokens occur at the same
    * position, a DismaxQuery will be created in this position and the tokens
    * constitute its disjuncts. The tiebreak factor will be set to the
    * dmqTieBreakerMultiplier property of this LuceneQueryBuilder.
    * </p>
    * 
    * 
    * @param fieldname
    * @param boost
    * @param target
    * @param sourceTerm
    * @throws IOException
    */
    void addTerm(final String fieldname, final FieldBoost boost, final DisjunctionMaxQueryFactory target,
                 final Term sourceTerm) throws IOException {
        final TermSubQueryFactory queryFactory = termSubQueryBuilder.termToFactory(fieldname, sourceTerm, boost);//termToFactory(fieldname, sourceTerm, boost);
        if (queryFactory != null) {
            target.add(queryFactory);
            boost.registerTermSubQuery(fieldname, queryFactory, sourceTerm);
        }
    }

}
