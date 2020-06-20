/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.SearchEngineRequestAdapter;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class DecorateInstruction implements Instruction {
    
    public static final String DECORATION_CONTEXT_KEY = "querqy.commonrules.decoration";
    public static final String DECORATION_CONTEXT_MAP_KEY = "querqy.commonrules.decoration.map";

    protected final String decorationKey;
    protected final Object decorationValue;
    
    public DecorateInstruction(final Object decorationValue) {
        this(null, decorationValue);
    }

    public DecorateInstruction(final String decorationKey, final Object decorationValue) {
        if (decorationValue == null) {
            throw new IllegalArgumentException("decorationValue must not be null");
        }

        this.decorationKey = decorationKey;
        this.decorationValue = decorationValue;
    }

    /* (non-Javadoc)
     * @see querqy.rewrite.commonrules.model.Instruction#apply(querqy.rewrite.commonrules.model.PositionSequence, querqy.rewrite.commonrules.model.TermMatches, int, int, querqy.model.ExpandedQuery, java.util.Map)
     */
    @Override
    public void apply(final PositionSequence<Term> sequence, final TermMatches termMatches,
            final int startPosition, final int endPosition, final ExpandedQuery expandedQuery,
                      final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        if (this.decorationKey == null) {
            @SuppressWarnings("unchecked")
            Set<Object> decorations = (Set<Object>) searchEngineRequestAdapter.getContext().get(DECORATION_CONTEXT_KEY);
            if (decorations == null) {
                decorations = new HashSet<>();
                searchEngineRequestAdapter.getContext().put(DECORATION_CONTEXT_KEY, decorations);
            }

            decorations.add(decorationValue);

        } else {
            @SuppressWarnings("unchecked")
            Map<String, Object> decorationsMap = (Map<String, Object>) searchEngineRequestAdapter.getContext()
                    .get(DECORATION_CONTEXT_MAP_KEY);

            if (decorationsMap == null) {
                decorationsMap = new HashMap<>();
                searchEngineRequestAdapter.getContext().put(DECORATION_CONTEXT_MAP_KEY, decorationsMap);
            }

            decorationsMap.putIfAbsent(decorationKey, decorationValue);
        }
    }

    @Override
    public Set<Term> getGenerableTerms() {
        return QueryRewriter.EMPTY_GENERABLE_TERMS;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        return prime + decorationValue.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final DecorateInstruction other = (DecorateInstruction) obj;
        return decorationValue.equals(other.decorationValue);

    }

    @Override
    public String toString() {
        return "DecorateInstruction [decorationValue=" + decorationValue + "]";
    }

    

    
}
