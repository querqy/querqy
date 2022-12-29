/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.Set;

import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.SearchEngineRequestAdapter;

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
    * @param termMatches The terms that match the input condition of the rule
    * @param expandedQuery The query to rewrite
    * @param searchEngineRequestAdapter Access to the request context
    */
   void apply(TermMatches termMatches, ExpandedQuery expandedQuery, SearchEngineRequestAdapter searchEngineRequestAdapter);
   
   Set<Term> getGenerableTerms();

   InstructionDescription getInstructionDescription();


}
