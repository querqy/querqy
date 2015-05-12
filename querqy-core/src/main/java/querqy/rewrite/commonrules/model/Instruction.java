/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.Map;
import java.util.Set;

import querqy.model.ExpandedQuery;
import querqy.model.Term;

/**
 * 
 * A single right-hand side clause of a rewrite rule. It represents one of
 * possibly many actions that should be taken if the input matches the rule
 * condition(s).
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
    * @param context
    */
   void apply(PositionSequence<Term> sequence, TermMatches termMatches, int startPosition, int endPosition,
         ExpandedQuery expandedQuery,  Map<String, Object> context);
   
   Set<Term> getGenerableTerms();

}
