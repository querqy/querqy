/**
 * 
 */
package qp.model;


/**
 * @author rene
 *
 */
public class BooleanQuery extends SubQuery<BooleanClause> implements DisjunctionMaxClause, BooleanClause {
	
	public enum Operator {
		
		NONE(""), AND("AND"), OR("OR");
		final String txt;
		
		private Operator(String txt) {
			this.txt = txt;
		}
		
		@Override
		public String toString() {
			return txt;
		}

		
	}
	
	protected Operator operator = Operator.NONE;
	
	public Operator getOperator() {
		return operator;
	}


	
	public BooleanQuery(Operator operator, Occur occur) {
		super(occur);
		this.operator = operator;
	}
	

	@Override
	public <T> T accept(NodeVisitor<T> visitor) {
		return visitor.visit(this);
	}



	@Override
	public String toString() {
		return "BooleanQuery [operator=" + operator + ", occur=" + occur
				+ ", clauses=" + clauses + "]";
	}

}
