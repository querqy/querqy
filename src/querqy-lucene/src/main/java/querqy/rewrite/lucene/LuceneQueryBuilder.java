/**
 * 
 */
package querqy.rewrite.lucene;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.UnicodeUtil;

import querqy.model.AbstractNodeVisitor;
import querqy.model.BooleanQuery;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Term;

/**
 * @author rene
 *
 */
public class LuceneQueryBuilder extends AbstractNodeVisitor<Query> {
    
    enum ParentType {BQ, DMQ}
    
    final Analyzer analyzer;
    final Map<String, Float> searchFieldsAndBoostings;
    final IndexStats indexStats;
    LinkedList<LinkedList<BooleanClause>> clauseStack = new LinkedList<>();
    LinkedList<LinkedList<Query>> subQueryStack = new LinkedList<>();
    
    protected ParentType parentType = ParentType.BQ;
    
    public LuceneQueryBuilder(Analyzer analyzer, Map<String, Float> searchFieldsAndBoostings, IndexStats indexStats) {
        this.analyzer = analyzer;
        this.searchFieldsAndBoostings = searchFieldsAndBoostings;
        this.indexStats = indexStats;
    }

    public Query createQuery(querqy.model.Query query) {
        return visit(query);
    }
    
    @Override
    public Query visit(querqy.model.Query query) {
        parentType = ParentType.BQ;
        return visit((BooleanQuery) query);
    }
    
    @Override
    public Query visit(BooleanQuery booleanQuery) {
        
        ParentType myParentType = parentType;
        parentType = ParentType.BQ;
        
        LinkedList<BooleanClause> clauses = new LinkedList<>();
        
        clauseStack.add(clauses);
        super.visit(booleanQuery);
        clauseStack.removeLast();
        
        parentType = myParentType;
        
        BooleanClause result = null;
        
        switch (clauses.size()) {
        case 0: throw new IllegalArgumentException("No subqueries found for BQ. Parent: " + booleanQuery.getParentQuery());
        case 1: 
            result = clauses.getFirst();
            break;
        default:
            org.apache.lucene.search.BooleanQuery bq = new org.apache.lucene.search.BooleanQuery();
            for (BooleanClause clause: clauses) {
                bq.add(clause);
            }
            
            result = new BooleanClause(bq, occur(booleanQuery.occur));
        }
        
        Query query = result.getQuery();
        
        switch (parentType) {
        case BQ:
            if (!clauseStack.isEmpty()) {
                clauseStack.getLast().add(result);
            } // else we are the top BQ
            return query;
        case DMQ:
            if (result.getOccur() != Occur.SHOULD) {
                // create a wrapper query
                org.apache.lucene.search.BooleanQuery bq = new org.apache.lucene.search.BooleanQuery(true);
                bq.add(result);
                query = bq;
            }
//            if (clauses.size() > 1) {
//                query.setBoost(1f / (float) clauses.size());
//            }
            subQueryStack.getLast().add(query);
            return query;
          
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
    public Query visit(DisjunctionMaxQuery disjunctionMaxQuery) {
        
        ParentType myParentType = parentType;
        parentType = ParentType.DMQ;
        
        LinkedList<Query> subQueries = new LinkedList<>();
        
        subQueryStack.add(subQueries);
        super.visit(disjunctionMaxQuery);
        subQueryStack.removeLast();
        
        parentType = myParentType;
        
        switch (subQueries.size()) {
        case 0: throw new IllegalArgumentException("No subqueries found for DMQ. Parent: " + disjunctionMaxQuery.getParentQuery());
        case 1: 
            Query child =  subQueries.getFirst();
            clauseStack.getLast().add(new BooleanClause(child, occur(disjunctionMaxQuery.occur)));
            return child;
        default:
            
            if (disjunctionMaxQuery.occur == querqy.model.SubQuery.Occur.MUST_NOT) {
                org.apache.lucene.search.BooleanQuery bq = new org.apache.lucene.search.BooleanQuery();
                for (Query q : subQueries) {
                    bq.add(q, Occur.SHOULD);
                }
                clauseStack.getLast().add(new BooleanClause(bq, Occur.MUST_NOT));
                return bq;
            }
            
            org.apache.lucene.search.DisjunctionMaxQuery dmq = new org.apache.lucene.search.DisjunctionMaxQuery(0f);
            dmq.add(subQueries);
            clauseStack.getLast().add(new BooleanClause(dmq, occur(disjunctionMaxQuery.occur)));
            return dmq; 
        }
    }
    
    @Override
    public Query visit(Term term) {
        
        LinkedList<Query> siblings = subQueryStack.getLast();
        String fieldname = term.getField();

        try {
        
            if (fieldname != null) {
                Float boost = searchFieldsAndBoostings.get(fieldname);
                if (boost != null) {
                    addTerm(fieldname, boost, term.reader(), siblings, term);
                } else {
                    // someone searches in a field that is not set as a search field 
                    // --> set value to fieldname + ":" + value in search in all fields
                    String value = fieldname + ":" + term.getValue();
                    for (Map.Entry<String, Float> field: searchFieldsAndBoostings.entrySet()) {
                        addTerm(field.getKey(), field.getValue(), new StringReader(value), siblings, term);
                    }
                }
            } else {
                for (Map.Entry<String, Float> field: searchFieldsAndBoostings.entrySet()) {
                    addTerm(field.getKey(), field.getValue(), term.reader(), siblings, term);
                }
            }
        } catch (IOException e) {
            // REVISIT: throw more specific exception?
            // - or save exception in Builder and then throw IOException from build()
            throw new RuntimeException(e);
        }
        
        return null;
        
    }
    
    void addTerm(String fieldname, float boost, Reader reader, List<Query> target, Term sourceTerm) throws IOException {
        
        LinkedList<org.apache.lucene.index.Term> backList = new LinkedList<>();
      //  reader.reset();
        TokenStream ts = null;
        try {
            ts = analyzer.tokenStream(fieldname, reader);
            CharTermAttribute termAttr = ts.addAttribute(CharTermAttribute.class);
            PositionIncrementAttribute posIncAttr = ts.addAttribute(PositionIncrementAttribute.class);
            ts.reset();
            while (ts.incrementToken()) {
                
                int inc = posIncAttr.getPositionIncrement();
                
                if (inc > 0) {
                    applyBackList(boost, backList, target, sourceTerm);
                }            
                
                int length = termAttr.length();
                BytesRef bytes = new BytesRef(length * 4);
                UnicodeUtil.UTF16toUTF8(termAttr.buffer(), 0, length, bytes);
                backList.add(new org.apache.lucene.index.Term(fieldname, bytes));
            }
            applyBackList(boost, backList, target, sourceTerm);
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
    void applyBackList(float boost, LinkedList<org.apache.lucene.index.Term> backList, List<Query> target, Term sourceTerm) {
       // String rewriteSourceValue = rewriteRoot.getValue(); 
    //    System.out.println("REWRITE R " + rewriteRoot);
      //  System.out.println("CURRENT T " + currentTerm);
        
        int backListSize = backList.size();
        
        if (backListSize > 0)  {
            
            Term rewriteRoot = sourceTerm.getRewriteRoot();
            BytesRef rewriteSourceValue = new BytesRef();
            UnicodeUtil.UTF16toUTF8(rewriteRoot.getChars(), 0, rewriteRoot.length, rewriteSourceValue);
            String rewriteSourceField = rewriteRoot.getField();
            
            if (backListSize == 1) {
                org.apache.lucene.index.Term term = backList.removeFirst();
                TermQuery tq = null;
                // FIXME
                if (rewriteRoot == sourceTerm || termEquals(term, rewriteSourceField, rewriteSourceValue)) {
                    tq = new TermQuery(term);
                } else {
                    int df = indexStats.df(new org.apache.lucene.index.Term(term.field(), rewriteSourceValue));
                    if (df == 0) {
                        tq = new TermQuery(term);
                    } else {
                        tq = new TermQuery(term, df);
                    }
                }
                tq.setBoost(boost);
                target.add(tq);
                
            } else {
                
                org.apache.lucene.search.DisjunctionMaxQuery dmq = new org.apache.lucene.search.DisjunctionMaxQuery(0f);
                dmq.setBoost(boost);
                while (!backList.isEmpty()) {
                    org.apache.lucene.index.Term term = backList.removeFirst();
                    TermQuery tq = null;
                   
                    if (rewriteRoot == sourceTerm || termEquals(term, rewriteSourceField, rewriteSourceValue)) { //|| term.field().equals(rewriteSourceField)) {
                        tq = new TermQuery(term);
                    } else {
                        int df = indexStats.df(new org.apache.lucene.index.Term(term.field(), rewriteSourceValue));
                        if (df == 0) {
                            tq = new TermQuery(term);
                        } else {
                            tq = new TermQuery(term, df);
                        }
                    }
                    dmq.add(tq);
                }
                target.add(dmq);
            }
        }
    }

}
