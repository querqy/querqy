/**
 * 
 */
package querqy.model;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class BooleanQuery extends SubQuery<BooleanParent, BooleanClause> implements DisjunctionMaxClause,
      BooleanClause, BooleanParent, QuerqyQuery<BooleanParent> {

   public BooleanQuery(BooleanParent parentQuery, Occur occur, boolean generated) {
      super(parentQuery, occur, generated);
   }

   @Override
   public <T> T accept(NodeVisitor<T> visitor) {
      return visitor.visit(this);
   }

   @Override
   public String toString() {
      return "BooleanQuery [occur=" + occur
            + ", clauses=" + clauses + "]";
   }

   @Override
   public BooleanQuery clone(BooleanParent newParent) {
      BooleanQuery bq = new BooleanQuery(newParent, occur, generated);
      for (BooleanClause clause : clauses) {
         bq.addClause(clause.clone(bq));
      }
      return bq;
   }

   @Override
   public BooleanQuery clone(DisjunctionMaxQuery newParent, boolean generated) {
      return clone((BooleanParent) newParent, generated);
   }

   @Override
   public BooleanClause clone(BooleanQuery newParent) {
      return clone((BooleanParent) newParent);
   }

   @Override
   public BooleanQuery clone(BooleanParent newParent, boolean generated) {
       BooleanQuery bq = new BooleanQuery(newParent, occur, generated);
       for (BooleanClause clause : clauses) {
          bq.addClause(clause.clone(bq, generated));
       }
       return bq;
   }

   @Override
   public BooleanClause clone(BooleanQuery newParent, boolean generated) {
       return clone((BooleanParent) newParent, generated);
   }

}
