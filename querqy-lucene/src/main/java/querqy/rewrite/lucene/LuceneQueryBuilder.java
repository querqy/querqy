/**
 * 
 */
package querqy.rewrite.lucene;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.input.CharSequenceReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;

import querqy.CompoundCharSequence;
import querqy.model.AbstractNodeVisitor;
import querqy.model.BooleanQuery;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Term;
import querqy.rewrite.commonrules.model.PositionSequence;
import querqy.rewrite.lucene.BooleanQueryFactory.Clause;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class LuceneQueryBuilder extends AbstractNodeVisitor<LuceneQueryFactory<?>> {
    
    enum ParentType {BQ, DMQ}
    
    final Analyzer analyzer;
    final Map<String, Float> searchFieldsAndBoostings;
    final IndexStats indexStats;
    final boolean normalizeBooleanQueryBoost;
    final float dmqTieBreakerMultiplier;
    final float generatedFieldBoostFactor;
    final IndexSearcher indexSearcher;
    
    LinkedList<BooleanQueryFactory> clauseStack = new LinkedList<>();
    LinkedList<DisjunctionMaxQueryFactory> subQueryStack = new LinkedList<>();
    
    protected ParentType parentType = ParentType.BQ;
    
    public LuceneQueryBuilder(IndexSearcher indexSearcher, Analyzer analyzer, 
    		Map<String, Float> searchFieldsAndBoostings, 
    		IndexStats indexStats, 
    		float dmqTieBreakerMultiplier) {
        this(indexSearcher, analyzer, searchFieldsAndBoostings, indexStats, dmqTieBreakerMultiplier, 1f, true);
    }

    public LuceneQueryBuilder(IndexSearcher indexSearcher, Analyzer analyzer, 
    		Map<String, Float> searchFieldsAndBoostings, 
    		IndexStats indexStats, 
    		float dmqTieBreakerMultiplier, float generatedFieldBoostFactor) {
        this(indexSearcher, analyzer, searchFieldsAndBoostings, indexStats, dmqTieBreakerMultiplier, generatedFieldBoostFactor, true);
    }
    
    public LuceneQueryBuilder(IndexSearcher indexSearcher, Analyzer analyzer, Map<String, Float> searchFieldsAndBoostings, IndexStats indexStats, 
    		float dmqTieBreakerMultiplier, float generatedFieldBoostFactor, boolean normalizeBooleanQueryBoost) {
        this.analyzer = analyzer;
        this.searchFieldsAndBoostings = searchFieldsAndBoostings;
        this.indexStats = indexStats;
        this.dmqTieBreakerMultiplier = dmqTieBreakerMultiplier;
        this.normalizeBooleanQueryBoost = normalizeBooleanQueryBoost;
        this.generatedFieldBoostFactor = generatedFieldBoostFactor;
        this.indexSearcher = indexSearcher;
    }
    
    public void reset() {
    	clauseStack.clear();
    	subQueryStack.clear();
    }

    public Query createQuery(querqy.model.Query query) throws IOException {
        return visit(query).createQuery(-1, indexStats);
    }
    
    @Override
    public LuceneQueryFactory<?> visit(querqy.model.Query query) {
        parentType = ParentType.BQ;
        return visit((BooleanQuery) query);
    }
    
    @Override
    public LuceneQueryFactory<?> visit(BooleanQuery booleanQuery) {
        
        
        BooleanQueryFactory bq = new BooleanQueryFactory(1f, booleanQuery.isGenerated(), normalizeBooleanQueryBoost && parentType == ParentType.DMQ); // FIXME: boost param?

        ParentType myParentType = parentType;
        parentType = ParentType.BQ;
        
        clauseStack.add(bq);
        super.visit(booleanQuery);
        clauseStack.removeLast();
        
        parentType = myParentType;
        
        Clause result = null;
        
        switch (bq.getNumberOfClauses()) {
        case 0: throw new IllegalArgumentException("No subqueries found for BQ. Parent: " + booleanQuery.getParent());
        case 1: 
            result = bq.getFirstClause(); 
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
                BooleanQueryFactory wrapper = new BooleanQueryFactory(1f, true, false);
                wrapper.add(result);
                bq = wrapper;
            }
            subQueryStack.getLast().add(bq);
            return bq;
          
        default:
            throw new RuntimeException("Unknown parentType " + parentType);
        }
    }
    
    protected Occur occur(querqy.model.SubQuery.Occur occur) {
        switch (occur) {
        case MUST: return Occur.MUST;
        case MUST_NOT: return Occur.MUST_NOT;
        case SHOULD: return Occur.SHOULD;
        }
        throw new IllegalArgumentException("Cannot handle occur value: " + occur.name());
    }
    
    @Override
    public LuceneQueryFactory<?> visit(DisjunctionMaxQuery disjunctionMaxQuery) {
        
        ParentType myParentType = parentType;
        parentType = ParentType.DMQ;
        
        DisjunctionMaxQueryFactory dmq = new DisjunctionMaxQueryFactory(1f, dmqTieBreakerMultiplier); // FIXME params
        
        subQueryStack.add(dmq);
        super.visit(disjunctionMaxQuery);
        subQueryStack.removeLast();
        
        parentType = myParentType;
        
        switch (dmq.getNumberOfDisjuncts()) {
        case 0: throw new IllegalArgumentException("No subqueries found for DMQ. Parent: " + disjunctionMaxQuery.getParent());
        case 1: 
            LuceneQueryFactory<?> firstDisjunct = dmq.getFirstDisjunct();
            clauseStack.getLast().add(firstDisjunct, occur(disjunctionMaxQuery.occur));
            return firstDisjunct;
        default:
            // FIXME: we can decide this earlier --> avoid creating DMQ in case of MUST_NOT
            if (disjunctionMaxQuery.occur == querqy.model.SubQuery.Occur.MUST_NOT) {
                // FIXME: correct to normalize boost?
                BooleanQueryFactory bq = new BooleanQueryFactory(1f, true, false);
                for (LuceneQueryFactory<?> queryFactory : dmq.disjuncts) {
                    bq.add(queryFactory, Occur.SHOULD);
                }
                clauseStack.getLast().add(bq, Occur.MUST_NOT);
                return bq;
            }
            
            clauseStack.getLast().add(dmq, occur(disjunctionMaxQuery.occur) );
            return dmq; 
        }
    }
    
    @Override
    public LuceneQueryFactory<?> visit(Term term) {
        
        DisjunctionMaxQueryFactory siblings = subQueryStack.getLast();
        String fieldname = term.getField();

        try {
        
            if (fieldname != null) {
                Float boost = searchFieldsAndBoostings.get(fieldname);
                if (boost != null) {
                    addTerm(fieldname, boost, siblings, term);
                } else {
                    // someone searches in a field that is not set as a search field 
                    // --> set value to fieldname + ":" + value in search in all fields
                    Term termWithFieldInValue = new Term(null, new CompoundCharSequence(":", fieldname, term.getValue()));
                    for (Map.Entry<String, Float> field: searchFieldsAndBoostings.entrySet()) {
                        addTerm(field.getKey(), field.getValue(), siblings, termWithFieldInValue);
                    }
                }
            } else {
                for (Map.Entry<String, Float> field: searchFieldsAndBoostings.entrySet()) {
                    addTerm(field.getKey(), field.getValue(), siblings, term);
                }
            }
        } catch (IOException e) {
            // REVISIT: throw more specific exception?
            // - or save exception in Builder and then throw IOException from build()
            throw new RuntimeException(e);
        }
        
        return null;
        
    }
    
    /**
   	 * 
   	 * <p>Applies analysis to a term and adds the result to the Lucene query factory tree.</p>
   	 * 
   	 * <p>The analysis might emit multiple tokens for the input term. If these tokens constitute a sequence (according
   	 * to the position attribute), a BooleanQuery will be created and each position in the sequence constitutes a 
   	 * MUST clause of this BooleanQuery. If multiple tokens occur at the same position, a DismaxQuery will be created in
   	 * this position and the tokens constitute its disjuncts. The tiebreak factor will be set to the dmqTieBreakerMultiplier property
   	 * of this LuceneQueryBuilder.</p>
   	 *  
   	 * 
     * @param fieldname
     * @param boost
     * @param target
     * @param sourceTerm
     * @throws IOException
     */
    void addTerm(String fieldname, float boost, DisjunctionMaxQueryFactory target, Term sourceTerm) throws IOException {
    	
    	float applicableBoost = sourceTerm.isGenerated() ? generatedFieldBoostFactor * boost : boost;
    	
    	PositionSequence<org.apache.lucene.index.Term> sequence = new PositionSequence<>();
    	
        TokenStream ts = null;
        try {
            ts = analyzer.tokenStream(fieldname, new CharSequenceReader(sourceTerm));
            CharTermAttribute termAttr = ts.addAttribute(CharTermAttribute.class);
            PositionIncrementAttribute posIncAttr = ts.addAttribute(PositionIncrementAttribute.class);
            ts.reset();
            while (ts.incrementToken()) {
                
                int inc = posIncAttr.getPositionIncrement();
                if (inc > 0 || sequence.isEmpty()) {
                	sequence.nextPosition();
                }
                
                sequence.addElement(new org.apache.lucene.index.Term(fieldname, new BytesRef(termAttr)));
            }
            
            
            if (sequence.size() == 1) {
            	target.add(getLuceneQueryFactoryForStreamPosition(sequence.getFirst(), applicableBoost));
            } else {
            	BooleanQueryFactory bq = new BooleanQueryFactory(boost, true, true);
	            for (List<org.apache.lucene.index.Term> posTerms : sequence) {
	            	bq.add(getLuceneQueryFactoryForStreamPosition(posTerms, applicableBoost), Occur.MUST);
	            }
	            target.add(bq);
            }
            
        } finally {
            if (ts != null) {
                try {
                    ts.close();
                } catch (IOException e) {}
            }
        }
        
    }
    
    protected LuceneQueryFactory<?> getLuceneQueryFactoryForStreamPosition(List<org.apache.lucene.index.Term> posTerms, float boost) {
    	if (posTerms.size() == 1) {
    		return new TermQueryFactory(posTerms.get(0), boost, indexSearcher);
    	} else {
    		// TODO: use tiebreak = 0 ?
    		DisjunctionMaxQueryFactory dmq = new DisjunctionMaxQueryFactory(boost, dmqTieBreakerMultiplier);
    		for (org.apache.lucene.index.Term term: posTerms) {
    			dmq.add(new TermQueryFactory(term, 1f, indexSearcher));
    		}
            return dmq; 
    	}
    }
    

}
