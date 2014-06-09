/**
 * 
 */
package querqy.rewrite.lucene;

import java.util.LinkedList;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

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
        String value = term.getValue();
        
        if (fieldname != null) {
            Float boost = searchFieldsAndBoostings.get(fieldname);
            if (boost != null) {
                TermQuery tq = new TermQuery(new org.apache.lucene.index.Term(fieldname, value));
                tq.setBoost(boost);
                siblings.add(tq);
            } else {
                // someone search in a field that is not set as a search field 
                // --> set value to fieldname + ":" + value in search in all fields
                value = fieldname + ":" + value;
                for (Map.Entry<String, Float> field: searchFieldsAndBoostings.entrySet()) {
                    TermQuery tq = new TermQuery(new org.apache.lucene.index.Term(field.getKey(), value));
                    tq.setBoost(field.getValue());
                    siblings.add(tq);
                }
            }
        } else {
            for (Map.Entry<String, Float> field: searchFieldsAndBoostings.entrySet()) {
                TermQuery tq = new TermQuery(new org.apache.lucene.index.Term(field.getKey(), value));
                tq.setBoost(field.getValue());
                siblings.add(tq);
            }
        }
        
        return null;
        
    }

}
