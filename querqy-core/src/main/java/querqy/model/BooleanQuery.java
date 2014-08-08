/**
 * 
 */
package querqy.model;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class BooleanQuery extends SubQuery<BooleanParent, BooleanClause> implements DisjunctionMaxClause, BooleanClause, BooleanParent {
	
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

}
