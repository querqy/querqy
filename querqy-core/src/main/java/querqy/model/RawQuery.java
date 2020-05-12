/**
 * 
 */
package querqy.model;

/**
 * @author René Kriegler, @renekrie
 *
 */
public abstract class RawQuery extends Clause<BooleanParent> implements QuerqyQuery<BooleanParent> {

   public RawQuery(final BooleanParent parent, final Occur occur, final boolean isGenerated) {
      super(parent, occur, isGenerated);
   }

   @Override
   public <T> T accept(final NodeVisitor<T> visitor) {
      return visitor.visit(this);
   }


}
