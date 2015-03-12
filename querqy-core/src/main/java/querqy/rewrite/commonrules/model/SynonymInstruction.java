/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.List;
import java.util.Map;

import querqy.ComparableCharSequence;
import querqy.model.BooleanQuery;
import querqy.model.Clause.Occur;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Term;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class SynonymInstruction implements Instruction {
    
    final List<querqy.rewrite.commonrules.model.Term> synonym;

    /**
     * 
     */
    public SynonymInstruction(List<querqy.rewrite.commonrules.model.Term> synonym) {
        if (synonym == null || synonym.isEmpty()) {
            throw new IllegalArgumentException("Synonym expansion required");
        }
        
        this.synonym = synonym;
    }

    /* (non-Javadoc)
     * @see querqy.rewrite.commonrules.model.Instruction#apply(querqy.rewrite.commonrules.model.PositionSequence, querqy.rewrite.commonrules.model.TermMatches, int, int, querqy.model.ExpandedQuery, java.util.Map)
     */
    @Override
    public void apply(PositionSequence<Term> sequence, TermMatches termMatches,
            int startPosition, int endPosition, ExpandedQuery expandedQuery,  Map<String, Object> context) {
        
        switch (termMatches.size()) {
            
        case 0: throw new IllegalArgumentException("termMatches must not be empty");
        case 1: {
            Term match = termMatches.get(0).getQueryTerm();
            DisjunctionMaxQuery parent = match.getParent();
            
            if (synonym.size() == 1) {
                addSynonymTermToDisjunctionMaxQuery(parent, synonym.get(0), termMatches);
                
            } else {
                
                BooleanQuery bq = new BooleanQuery(match.getParent(), Occur.SHOULD, true);
                match.getParent().addClause(bq);
                for (querqy.rewrite.commonrules.model.Term synTerm: synonym) {
                    DisjunctionMaxQuery dmq = new  DisjunctionMaxQuery(bq, Occur.MUST, true);
                    bq.addClause(dmq);
                    addSynonymTermToDisjunctionMaxQuery(dmq, synTerm, termMatches);
                }
                
            }
        }
        break;
        default:
            for (TermMatch match: termMatches) {
                
                DisjunctionMaxQuery clauseDmq = match.getQueryTerm().getParent();
                
                if (synonym.size() == 1) {
                    addSynonymTermToDisjunctionMaxQuery(clauseDmq, synonym.get(0), termMatches);
                } else {
                    BooleanQuery bq = new BooleanQuery(clauseDmq, Occur.SHOULD, true);
                    clauseDmq.addClause(bq);
                    
                    for (querqy.rewrite.commonrules.model.Term synTerm: synonym) {
                        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(bq, Occur.MUST, true);
                        bq.addClause(dmq);
                        addSynonymTermToDisjunctionMaxQuery(dmq, synTerm, termMatches);
                    }
                
                }
                
            }
            
        }

    }
    
    protected void addSynonymTermToDisjunctionMaxQuery(DisjunctionMaxQuery dmq, querqy.rewrite.commonrules.model.Term synTerm, TermMatches termMatches) {
        List<String> fieldNames = synTerm.getFieldNames();
        ComparableCharSequence charSequence = synTerm.fillPlaceholders(termMatches);
        if (fieldNames == null || fieldNames.isEmpty()) {
            dmq.addClause(new Term(dmq, charSequence, true));
        } else {
            for (String fieldName: fieldNames) {
                dmq.addClause(new Term(dmq, fieldName, charSequence, true));
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((synonym == null) ? 0 : synonym.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SynonymInstruction other = (SynonymInstruction) obj;
        if (synonym == null) {
            if (other.synonym != null)
                return false;
        } else if (!synonym.equals(other.synonym))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SynonymInstruction [synonym=" + synonym + "]";
    }

    
}
