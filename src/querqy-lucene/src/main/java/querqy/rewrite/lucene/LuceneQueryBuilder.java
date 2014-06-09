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
import org.apache.lucene.util.automaton.UTF32ToUTF8;

import querqy.model.AbstractNodeVisitor;
import querqy.model.BooleanQuery;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.SubQuery;
import querqy.model.Term;

/**
 * @author rene
 *
 */
public class LuceneQueryBuilder extends AbstractNodeVisitor<Query> {
    
    final Analyzer analyzer;
    final Map<String, Float> searchFieldsAndBoostings;
    LinkedList<LinkedList<BooleanClause>> clauseStack = new LinkedList<>();
    LinkedList<LinkedList<Query>> subQueryStack = new LinkedList<>();
    
    
    public LuceneQueryBuilder(Analyzer analyzer, Map<String, Float> searchFieldsAndBoostings) {
        this.analyzer = analyzer;
        this.searchFieldsAndBoostings = searchFieldsAndBoostings;
    }

    public Query createQuery(querqy.model.Query query) {
        return visit(query);
    }
    
    @Override
    public Query visit(querqy.model.Query query) {
        
        return visit((BooleanQuery) query);
    }
    
    @Override
    public Query visit(BooleanQuery booleanQuery) {
        LinkedList<BooleanClause> clauses = new LinkedList<>();
        clauseStack.add(clauses);
        super.visit(booleanQuery);
        clauseStack.removeLast();
        
        switch (clauses.size()) {
        case 0: throw new IllegalArgumentException("No subqueries found for BQ. Parent: " + booleanQuery.getParentQuery());
        case 1: 
            BooleanClause child =  clauses.getFirst();
            if (!clauseStack.isEmpty()) {
                clauseStack.getLast().add(child);
            }
            return child.getQuery();
        default:
            org.apache.lucene.search.BooleanQuery bq = new org.apache.lucene.search.BooleanQuery();
            for (BooleanClause clause: clauses) {
                bq.add(clause);
            }
            if (!clauseStack.isEmpty()) {
                clauseStack.getLast().add(new BooleanClause(bq, occur(booleanQuery.occur)));
            }
            return bq; 
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
        LinkedList<Query> subQueries = new LinkedList<>();
        
        subQueryStack.add(subQueries);
        super.visit(disjunctionMaxQuery);
        subQueryStack.removeLast();
        
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
                    addTerm(fieldname, boost, term.reader(), siblings);
                } else {
                    // someone search in a field that is not set as a search field 
                    // --> set value to fieldname + ":" + value in search in all fields
                    String value = fieldname + ":" + term.getValue();
                    for (Map.Entry<String, Float> field: searchFieldsAndBoostings.entrySet()) {
                        addTerm(field.getKey(), field.getValue(), new StringReader(value), siblings);
                    }
                }
            } else {
                for (Map.Entry<String, Float> field: searchFieldsAndBoostings.entrySet()) {
                    addTerm(field.getKey(), field.getValue(), term.reader(), siblings);
                }
            }
        } catch (IOException e) {
            // REVISIT: throw more specific exception?
            // - or save exception in Builder and then throw IOException from build()
            throw new RuntimeException(e);
        }
        
        return null;
        
    }
    
    void addTerm(String fieldname, float boost, Reader reader, List<Query> target) throws IOException {
        
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
    
    void applyBackList(float boost, LinkedList<org.apache.lucene.index.Term> backList, List<Query> target) {
        switch (backList.size()) {
        case 0: break;
        case 1: 
            TermQuery tq = new TermQuery(backList.removeFirst());
            tq.setBoost(boost);
            target.add(tq);
            break;
        default:
            org.apache.lucene.search.DisjunctionMaxQuery dmq = new org.apache.lucene.search.DisjunctionMaxQuery(0f);
            dmq.setBoost(boost);
            while (!backList.isEmpty()) {
                dmq.add(new TermQuery(backList.removeFirst()));
            }
            target.add(dmq);
               
        }
    }

}
