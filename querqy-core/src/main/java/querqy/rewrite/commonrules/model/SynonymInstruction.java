/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import querqy.ComparableCharSequence;
import querqy.model.BooleanQuery;
import querqy.model.BoostedTerm;
import querqy.model.Clause.Occur;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.SearchEngineRequestAdapter;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class SynonymInstruction implements Instruction {
    
    public static final float DEFAULT_TERM_BOOST = 1f;

    final List<querqy.rewrite.commonrules.model.Term> synonym;
    final float boost;
    private final InstructionDescription instructionDescription;

    /**
     * @param synonym The terms of the synonym expansion.
     * @param boost A boost factor of the synonym &gt;= 0f
     */
    public SynonymInstruction(final List<querqy.rewrite.commonrules.model.Term> synonym,
                              final float boost,
                              final InstructionDescription instructionDescription) {
        if (synonym == null || synonym.isEmpty()) {
            throw new IllegalArgumentException("Synonym expansion required");
        }
        if (boost < 0f) {
            throw new IllegalArgumentException("Negative Synonym boosts not allowed");
        }
        
        this.synonym = synonym;
        this.boost = boost;
        this.instructionDescription = instructionDescription;
    }
    
    /**
     * @param synonym The terms of the synonym expansion.
     */
    @Deprecated // Do not use for non-test purposes
    public SynonymInstruction(final List<querqy.rewrite.commonrules.model.Term> synonym) {
        this(synonym, DEFAULT_TERM_BOOST, InstructionDescription.empty());
    }

    /* (non-Javadoc)
     * @see querqy.rewrite.commonrules.model.Instruction#apply(querqy.rewrite.commonrules.model.PositionSequence, querqy.rewrite.commonrules.model.TermMatches, int, int, querqy.model.ExpandedQuery, java.util.Map)
     */
    @Override
    public void apply(final TermMatches termMatches, final ExpandedQuery expandedQuery,
                      final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        
        switch (termMatches.size()) {
            
        case 0: throw new IllegalArgumentException("termMatches must not be empty");
        case 1: {
            final Term match = termMatches.get(0).getQueryTerm();
            final DisjunctionMaxQuery parent = match.getParent();
            
            if (synonym.size() == 1) {
                addSynonymTermToDisjunctionMaxQuery(parent, synonym.get(0), termMatches);
                
            } else {

                final BooleanQuery bq = new BooleanQuery(match.getParent(), Occur.SHOULD, true);
                match.getParent().addClause(bq);
                for (final querqy.rewrite.commonrules.model.Term synTerm: synonym) {
                    final DisjunctionMaxQuery dmq = new  DisjunctionMaxQuery(bq, Occur.MUST, true);
                    bq.addClause(dmq);
                    addSynonymTermToDisjunctionMaxQuery(dmq, synTerm, termMatches);
                }
                
            }
        }
        break;
        default:
            for (final TermMatch match: termMatches) {

                final DisjunctionMaxQuery clauseDmq = match.getQueryTerm().getParent();
                
                if (synonym.size() == 1) {
                    addSynonymTermToDisjunctionMaxQuery(clauseDmq, synonym.get(0), termMatches);
                } else {
                    final BooleanQuery bq = new BooleanQuery(clauseDmq, Occur.SHOULD, true);
                    clauseDmq.addClause(bq);
                    
                    for (final querqy.rewrite.commonrules.model.Term synTerm: synonym) {
                        final DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(bq, Occur.MUST, true);
                        bq.addClause(dmq);
                        addSynonymTermToDisjunctionMaxQuery(dmq, synTerm, termMatches);
                    }
                
                }
                
            }
            
        }

    }
    
    protected void addSynonymTermToDisjunctionMaxQuery(final DisjunctionMaxQuery dmq,
                                                       final querqy.rewrite.commonrules.model.Term synTerm,
                                                       final TermMatches termMatches) {
        final List<String> fieldNames = synTerm.getFieldNames();
        final ComparableCharSequence charSequence = synTerm.fillPlaceholders(termMatches);
        if (fieldNames == null || fieldNames.isEmpty()) {
            dmq.addClause(createTermFrom(dmq, charSequence, null));
        } else {
            for (final String fieldName: fieldNames) {
                dmq.addClause(createTermFrom(dmq, charSequence, fieldName));
            }
        }
    }

    protected Term createTermFrom(final DisjunctionMaxQuery dmq, final ComparableCharSequence charSequence, final String fieldName) {
        if (Float.compare(this.boost, 1f) != 0) {
            return new BoostedTerm(dmq, fieldName, charSequence, boost);
        } else {
            return new Term(dmq, fieldName, charSequence, true);
        }
    }

    @Override
    public Set<Term> getGenerableTerms() {
        final Set<Term> result = new HashSet<>();
        for (querqy.rewrite.commonrules.model.Term synTerm: synonym) {
            if (!synTerm.hasPlaceHolder()) {
                final List<String> fieldNames = synTerm.getFieldNames();
                if (fieldNames == null || fieldNames.isEmpty()) {
                    result.add(new Term(null, synTerm, true));
                } else {
                    for (final String fieldName: fieldNames) {
                        result.add(new Term(null, fieldName, synTerm, true));
                    }
                }
            }
        }
        return result;
    }

    @Override
    public InstructionDescription getInstructionDescription() {
        return instructionDescription;
    }

    public float getTermBoost() {
        return boost;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((synonym == null) ? 0 : synonym.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final SynonymInstruction other = (SynonymInstruction) obj;
        if (synonym == null) {
            if (other.synonym != null)
                return false;
        } else if (!synonym.equals(other.synonym))
            return false;
        return true;
    }

    @Override
    public String toString() {
        if (boost != DEFAULT_TERM_BOOST) {
            return "SynonymInstruction [boost=" + boost + ", synonym=" + synonym + "]";
        }
        return "SynonymInstruction [synonym=" + synonym + "]";
    }
}
