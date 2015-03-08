/**
 * 
 */
package querqy.model;

import java.util.List;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class DisjunctionMaxQuery extends SubQuery<BooleanQuery, DisjunctionMaxClause> implements BooleanClause,
      BooleanParent {

   public DisjunctionMaxQuery(BooleanQuery parentQuery, Occur occur, boolean generated) {
      super(parentQuery, occur, generated);
   }

   public List<Term> getTerms() {
      return getClauses(Term.class);
   }

   @Override
   public <T> T accept(NodeVisitor<T> visitor) {
      return visitor.visit(this);
   }

   @Override
   public String toString() {
      return "DisjunctionMaxQuery [occur=" + occur + ", clauses=" + clauses
            + "]";
   }

   @Override
   public BooleanClause clone(BooleanQuery newParent) {
       return clone(newParent, this.generated);
   }

   @Override
   public BooleanClause clone(BooleanQuery newParent, boolean generated) {
       DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(newParent, occur, generated);
       for (DisjunctionMaxClause clause : clauses) {
          dmq.addClause(clause.clone(dmq));
       }
       return dmq;
   }

}
