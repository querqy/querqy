/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.List;

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
     * @see querqy.rewrite.commonrules.model.Instruction#apply(querqy.rewrite.commonrules.model.PositionSequence, java.util.List, int, int, querqy.model.ExpandedQuery)
     */
    @Override
    public void apply(PositionSequence<Term> sequence, List<Term> matchedTerms,
            int startPosition, int endPosition, ExpandedQuery expandedQuery) {
        
        switch (matchedTerms.size()) {
            
        case 0: throw new IllegalArgumentException("matchedTerms must not be empty");
        case 1: {
            Term match = matchedTerms.get(0);
            DisjunctionMaxQuery parent = match.getParent();
            
            if (synonym.size() == 1) {
                addSynonymTermToDisjunctionMaxQuery(parent, synonym.get(0));
                
            } else {
                
                BooleanQuery bq = new BooleanQuery(match.getParent(), Occur.SHOULD, true);
                match.getParent().addClause(bq);
                for (querqy.rewrite.commonrules.model.Term synTerm: synonym) {
                    DisjunctionMaxQuery dmq = new  DisjunctionMaxQuery(bq, Occur.MUST, true);
                    bq.addClause(dmq);
                    addSynonymTermToDisjunctionMaxQuery(dmq, synTerm);
                }
                
            }
        }
        break;
        default:
            for (Term match: matchedTerms) {
                
                DisjunctionMaxQuery clauseDmq = match.getParent();
                
                BooleanQuery bq = new BooleanQuery(clauseDmq, Occur.SHOULD, true);
                clauseDmq.addClause(bq);
                
                for (querqy.rewrite.commonrules.model.Term synTerm: synonym) {
                    DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(bq, Occur.MUST, true);
                    bq.addClause(dmq);
                    addSynonymTermToDisjunctionMaxQuery(dmq, synTerm);
                }
                
                BooleanQuery bqNeg = new BooleanQuery(bq, Occur.MUST_NOT, true);
                bq.addClause(bqNeg);
                for (Term matchedTerm: matchedTerms) {
                    DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(bqNeg, Occur.MUST, true);
                    bqNeg.addClause(dmq);
                    dmq.addClause(matchedTerm.clone(dmq, true));
                }
            }
            
        }

    }
    
    protected void addSynonymTermToDisjunctionMaxQuery(DisjunctionMaxQuery dmq, querqy.rewrite.commonrules.model.Term synTerm) {
        List<String> fieldNames = synTerm.getFieldNames();
        if (fieldNames == null || fieldNames.isEmpty()) {
            dmq.addClause(new Term(dmq, (ComparableCharSequence) synTerm, true));
        } else {
            for (String fieldName: fieldNames) {
                dmq.addClause(new Term(dmq, fieldName, synTerm, true));
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
