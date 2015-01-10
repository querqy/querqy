/**
 * 
 */
package querqy.rewrite.commonrules.model;

import querqy.model.BoostQuery;
import querqy.model.ExpandedQuery;
import querqy.model.QuerqyQuery;
import querqy.model.Term;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class BoostInstruction implements Instruction {

   public enum BoostDirection {
      UP, DOWN
   }

   final QuerqyQuery<?> query;
   final BoostDirection direction;
   float boost;

   public BoostInstruction(QuerqyQuery<?> query, BoostDirection direction, float boost) {
      if (query == null) {
         throw new IllegalArgumentException("query must not be null");
      }

      if (direction == null) {
         throw new IllegalArgumentException("direction must not be null");
      }

      this.query = query;
      this.direction = direction;
      this.boost = boost;
   }

   /* (non-Javadoc)
    * @see querqy.rewrite.commonrules.model.Instruction#apply(querqy.rewrite.commonrules.model.PositionSequence, 
    *                           querqy.rewrite.commonrules.model.TermMatches, int, int, querqy.model.ExpandedQuery)
    */
   @Override
   public void apply(PositionSequence<Term> sequence, TermMatches termMatches,
           int startPosition, int endPosition, ExpandedQuery expandedQuery) {
      BoostQuery bq = new BoostQuery(query.clone(null), boost);
      if (direction == BoostDirection.DOWN) {
         expandedQuery.addBoostDownQuery(bq);
      } else {
         expandedQuery.addBoostUpQuery(bq);
      }

   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Float.floatToIntBits(boost);
      result = prime * result
            + ((direction == null) ? 0 : direction.hashCode());
      result = prime * result + ((query == null) ? 0 : query.hashCode());
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
      BoostInstruction other = (BoostInstruction) obj;
      if (Float.floatToIntBits(boost) != Float.floatToIntBits(other.boost))
         return false;
      if (direction != other.direction)
         return false;
      if (query == null) {
         if (other.query != null)
            return false;
      } else if (!query.equals(other.query))
         return false;
      return true;
   }

   @Override
   public String toString() {
      return "BoostInstruction [query=" + query + ", direction=" + direction
            + ", boost=" + boost + "]";
   }

}
