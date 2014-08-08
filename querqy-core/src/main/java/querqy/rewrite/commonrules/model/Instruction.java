/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.List;

import querqy.model.ExpandedQuery;
import querqy.model.Term;

/**
 * 
 * A single right-hand side clause of a rewrite rule. It represents one of possibly more actions
 * that should be taken if the input matches the rule condition(s).
 * 
 * @author René Kriegler, @renekrie
 *
 */
public interface Instruction {
    /**
     * 
     * @param sequence
     * @param matchedTerms
     * @param startPosition
     * @param endPosition
     * @param expandedQuery
     */
    void apply(PositionSequence<Term> sequence, List<Term> matchedTerms, int startPosition, int endPosition, ExpandedQuery expandedQuery);

}
