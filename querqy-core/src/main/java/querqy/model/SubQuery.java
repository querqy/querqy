/**
 * 
 */
package querqy.model;

import java.util.LinkedList;
import java.util.List;

/**
 * @author René Kriegler, @renekrie
 *
 */
public abstract class SubQuery<P extends Node, C extends Node> extends Clause<P> {

   protected final List<C> clauses = new LinkedList<>();

   public SubQuery(P parentQuery, boolean generated) {
      this(parentQuery, Occur.SHOULD, generated);
   }

   public SubQuery(P parentQuery, Occur occur, boolean generated) {
      super(parentQuery, occur, generated);
   }

   @SuppressWarnings("unchecked")
   public <T extends C> List<T> getClauses(Class<T> type) {
      List<T> result = new LinkedList<>();
      for (C clause : clauses) {
         if (type.equals(clause.getClass())) {
            result.add((T) clause);
         }
      }
      return result;
   }

   public void addClause(C clause) {
      if (clause.getParent() != this) {
         throw new IllegalArgumentException("This query is not a parent of " + clause);
      }
      clauses.add(clause);
   }

   public void removeClause(C clause) {
      if (clause.getParent() != this) {
         throw new IllegalArgumentException("This query is not a parent of " + clause);
      }
      clauses.remove(clause);
   }

   public List<C> getClauses() {
      return clauses;
   }

}
