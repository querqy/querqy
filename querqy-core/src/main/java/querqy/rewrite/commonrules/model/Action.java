/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.List;

import querqy.model.Term;

/**
 * An Action represents all Instructions triggered for specific input positions.
 * It references the sequence of query terms that matched the rule. As there can
 * be more than one term in a single position it's possible that more than one
 * sequence matched the input.
 * 
 * @author rene
 *
 */
public class Action {

   final List<Instructions> instructions;
   final List<Term> matchedTerms;
   final int startPosition;
   final int endPosition; // exclusive

   public Action(List<Instructions> instructions, List<Term> matchedTerms, int startPosition, int endPosition) {
      this.instructions = instructions;
      this.matchedTerms = matchedTerms;
      this.startPosition = startPosition;
      this.endPosition = endPosition;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + endPosition;
      result = prime * result
            + ((instructions == null) ? 0 : instructions.hashCode());
      result = prime * result + startPosition;
      result = prime * result + ((matchedTerms == null) ? 0 : matchedTerms.hashCode());
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
      Action other = (Action) obj;
      if (endPosition != other.endPosition)
         return false;
      if (instructions == null) {
         if (other.instructions != null)
            return false;
      } else if (!instructions.equals(other.instructions))
         return false;
      if (startPosition != other.startPosition)
         return false;
      if (matchedTerms == null) {
         if (other.matchedTerms != null)
            return false;
      } else if (!matchedTerms.equals(other.matchedTerms))
         return false;
      return true;
   }

   @Override
   public String toString() {
      return "Action [instructions=" + instructions + ", terms=" + matchedTerms
            + ", startPosition=" + startPosition + ", endPosition="
            + endPosition + "]";
   }

   public List<Instructions> getInstructions() {
      return instructions;
   }

   public List<Term> getMatchedTerms() {
      return matchedTerms;
   }

   public int getStartPosition() {
      return startPosition;
   }

   public int getEndPosition() {
      return endPosition;
   }

}
