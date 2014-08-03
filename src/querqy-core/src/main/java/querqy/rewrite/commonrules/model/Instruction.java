/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.List;

import querqy.model.Term;

/**
 * 
 * A single right-hand side clause of a rewrite rule. It represents one of possibly more actions
 * that should be taken if the input matches the rule condition(s).
 * 
 * @author rene
 *
 */
public interface Instruction {
    
    void apply(TermPositionSequence sequence, List<Term> matchedTerms, int startPosition, int endPosition);

}
