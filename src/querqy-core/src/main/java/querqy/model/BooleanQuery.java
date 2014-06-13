/**
 * 
 */
package querqy.model;


/**
 * @author rene
 *
 */
public class BooleanQuery extends SubQuery<BooleanClause> implements DisjunctionMaxClause, BooleanClause {
	
	public BooleanQuery(SubQuery<?> parentQuery, Occur occur, boolean generated) {
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
