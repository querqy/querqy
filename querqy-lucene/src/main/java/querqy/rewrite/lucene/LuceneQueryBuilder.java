/**
 * 
 */
package querqy.rewrite.lucene;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.UnicodeUtil;

import querqy.CompoundCharSequence;
import querqy.model.AbstractNodeVisitor;
import querqy.model.BooleanQuery;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Term;
import querqy.rewrite.lucene.BooleanQueryFactory.Clause;

import com.google.common.io.CharSource;

/**
 * @author rene
 *
 */
public class LuceneQueryBuilder extends AbstractNodeVisitor<LuceneQueryFactory<?>> {
    
    enum ParentType {BQ, DMQ}
    
    final Analyzer analyzer;
    final Map<String, Float> searchFieldsAndBoostings;
    final IndexStats indexStats;
    final boolean normalizeBooleanQueryBoost;
    
    LinkedList<BooleanQueryFactory> clauseStack = new LinkedList<>();
    LinkedList<DisjunctionMaxQueryFactory> subQueryStack = new LinkedList<>();
    
    protected ParentType parentType = ParentType.BQ;
    
    public LuceneQueryBuilder(Analyzer analyzer, Map<String, Float> searchFieldsAndBoostings, IndexStats indexStats) {
        this(analyzer, searchFieldsAndBoostings, indexStats, true);
    }

    
    public LuceneQueryBuilder(Analyzer analyzer, Map<String, Float> searchFieldsAndBoostings, IndexStats indexStats, 
            boolean normalizeBooleanQueryBoost) {
        this.analyzer = analyzer;
        this.searchFieldsAndBoostings = searchFieldsAndBoostings;
        this.indexStats = indexStats;
        this.normalizeBooleanQueryBoost = normalizeBooleanQueryBoost;
    }

    public Query createQuery(querqy.model.Query query) {
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
        case 0: throw new IllegalArgumentException("No subqueries found for BQ. Parent: " + booleanQuery.getParentQuery());
        case 1: 
            result = bq.getFirstClause(); 
            break;
        default:
//            BooleanQueryFactory bq = new BooleanQueryFactory(1f, booleanQuery.isGenerated()); // REVISIT: boost
           // org.apache.lucene.search.BooleanQuery bq = new org.apache.lucene.search.BooleanQuery();
//            for (BooleanClause clause: clauses) {
//                bq.add(clause);
//            }
            
            result = new Clause(bq, occur(booleanQuery.occur));
        }
        
        //Query query = result.getQuery();
        
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
                //org.apache.lucene.search.BooleanQuery bq = new org.apache.lucene.search.BooleanQuery(true);
                wrapper.add(result);
                bq = wrapper;
            }
//            if (normalizeBooleanQueryBoost && clauses.size() > 1) {
//                query.setBoost(1f / (float) clauses.size());
//            }
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
        
        DisjunctionMaxQueryFactory dmq = new DisjunctionMaxQueryFactory(1f, 0f); // FIXME params
        
        subQueryStack.add(dmq);
        super.visit(disjunctionMaxQuery);
        subQueryStack.removeLast();
        
        parentType = myParentType;
        
        switch (dmq.getNumberOfDisjuncts()) {
        case 0: throw new IllegalArgumentException("No subqueries found for DMQ. Parent: " + disjunctionMaxQuery.getParentQuery());
        case 1: 
            LuceneQueryFactory<?> firstDisjunct = dmq.getFirstDisjunct();
            clauseStack.getLast().add(firstDisjunct, occur(disjunctionMaxQuery.occur));
//            Query child =  subQueries.getFirst();
//            clauseStack.getLast().add(new BooleanClause(child, occur(disjunctionMaxQuery.occur)));
            return firstDisjunct;
        default:
            // FIXME: we can decide this earlier --> avoid creating DMQ in case of MUST_NOT
            if (disjunctionMaxQuery.occur == querqy.model.SubQuery.Occur.MUST_NOT) {
                // FIXME: correct to normalize boost?
                BooleanQueryFactory bq = new BooleanQueryFactory(1f, true, false);
                for (LuceneQueryFactory<?> queryFactory : dmq.disjuncts) {
                    bq.add(queryFactory, Occur.SHOULD);
                }
//                org.apache.lucene.search.BooleanQuery bq = new org.apache.lucene.search.BooleanQuery(true);
//                for (Query q : subQueries) {
//                    bq.add(q, Occur.SHOULD);
//                }
                clauseStack.getLast().add(bq, Occur.MUST_NOT);
                return bq;
            }
            
//            
//            org.apache.lucene.search.DisjunctionMaxQuery dmq = new org.apache.lucene.search.DisjunctionMaxQuery(0f);
//            dmq.add(subQueries);
            clauseStack.getLast().add(dmq, occur(disjunctionMaxQuery.occur) );// new BooleanClause(dmq, occur(disjunctionMaxQuery.occur)));
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
    
    void addTerm(String fieldname, float boost, DisjunctionMaxQueryFactory target, Term sourceTerm) throws IOException {
        
        LinkedList<org.apache.lucene.index.Term> backList = new LinkedList<>();
      //  reader.reset();
        TokenStream ts = null;
        try {
        	CharSource termSource = CharSource.wrap(sourceTerm);
            ts = analyzer.tokenStream(fieldname, termSource.openStream());
            CharTermAttribute termAttr = ts.addAttribute(CharTermAttribute.class);
            PositionIncrementAttribute posIncAttr = ts.addAttribute(PositionIncrementAttribute.class);
            ts.reset();
            while (ts.incrementToken()) {
                
                int inc = posIncAttr.getPositionIncrement();
                
                if (inc > 0) {
                    applyBackList(boost, backList, target);
                }            
                
                int length = termAttr.length();
                BytesRef bytes = new BytesRef(length * 4);
                UnicodeUtil.UTF16toUTF8(termAttr.buffer(), 0, length, bytes);
                backList.add(new org.apache.lucene.index.Term(fieldname, bytes));
            }
            applyBackList(boost, backList, target);
        } finally {
            if (ts != null) {
                try {
                    ts.close();
                } catch (IOException e) {}
            }
        }
        
    }
    
    protected boolean termEquals(org.apache.lucene.index.Term luceneTerm, String termField, BytesRef termValue) {
        
        String fieldname = luceneTerm.field();
        
        boolean fieldEqual = fieldname == termField
                || ((fieldname != null) && fieldname.equals(termField));
        
        if (!fieldEqual) return false;
        
        BytesRef luceneBytes = luceneTerm.bytes();
        
        return luceneBytes == termValue || (luceneBytes != null && luceneBytes.equals(termValue));
        
       
        
    }
    void applyBackList(float boost, LinkedList<org.apache.lucene.index.Term> backList, DisjunctionMaxQueryFactory target) {
        
        int backListSize = backList.size();
        
        if (backListSize > 0)  {
            
            if (backListSize == 1) {
                org.apache.lucene.index.Term term = backList.removeFirst();
                TermQueryFactory tq = new TermQueryFactory(term, boost);
                target.add(tq);
                
            } else {
                DisjunctionMaxQueryFactory dmq = new DisjunctionMaxQueryFactory(boost, 0f);
                while (!backList.isEmpty()) {
                    org.apache.lucene.index.Term term = backList.removeFirst();
                    TermQueryFactory tq = new TermQueryFactory(term, 1f);
                    dmq.add(tq);
                }
                target.add(dmq);
            }
        }
    }

}
