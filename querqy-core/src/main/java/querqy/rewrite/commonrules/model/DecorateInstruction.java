/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class DecorateInstruction implements Instruction {
    
    public static final String CONTEXT_KEY = "querqy.commonrules.decoration";
    
    protected final Object decorationValue;
    
    public DecorateInstruction(Object decorationValue) {
        if (decorationValue == null) {
            throw new IllegalArgumentException("decorationValue must not be null");
        }
        this.decorationValue = decorationValue;
    }

    /* (non-Javadoc)
     * @see querqy.rewrite.commonrules.model.Instruction#apply(querqy.rewrite.commonrules.model.PositionSequence, querqy.rewrite.commonrules.model.TermMatches, int, int, querqy.model.ExpandedQuery, java.util.Map)
     */
    @Override
    public void apply(PositionSequence<Term> sequence, TermMatches termMatches,
            int startPosition, int endPosition, ExpandedQuery expandedQuery,
            Map<String, Object> context) {
        
        @SuppressWarnings("unchecked")
        Set<Object> decorations = (Set<Object>) context.get(CONTEXT_KEY);
        if (decorations == null) {
            decorations = new HashSet<>();
            context.put(CONTEXT_KEY, decorations);
        }
        
        decorations.add(decorationValue);
        

    }

    @Override
    public Set<Term> getGenerableTerms() {
        return QueryRewriter.EMPTY_GENERABLE_TERMS;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((decorationValue == null) ? 0 : decorationValue.hashCode());
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
        DecorateInstruction other = (DecorateInstruction) obj;
        if (decorationValue == null) {
            if (other.decorationValue != null)
                return false;
        } else if (!decorationValue.equals(other.decorationValue))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "DecorateInstruction [decorationValue=" + decorationValue + "]";
    }

    

    
}
