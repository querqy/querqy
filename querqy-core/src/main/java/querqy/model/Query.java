/**
 * 
 */
package querqy.model;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class Query extends BooleanQuery implements QuerqyQuery<BooleanParent> {

   public Query() {
      super(null, Occur.SHOULD, false);
   }

   @Override
   public Query clone(BooleanParent newParent) {
      Query q = new Query();
      for (BooleanClause clause : clauses) {
         q.addClause(clause.clone(q));
      }
      return q;
   }
   
   @Override
   public Query clone(BooleanParent newParent, boolean generated) {
      Query q = new Query();
      for (BooleanClause clause : clauses) {
         q.addClause(clause.clone(q, generated));
      }
      return q;
   }
}
