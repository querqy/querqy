/**
 * 
 */
package qp.model;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * @author rene
 *
 */
public class BooleanQuery extends Clause {
	
	public enum Operator implements Node {
		
		NONE(""), AND("AND"), OR("OR");
		final String txt;
		
		private Operator(String txt) {
			this.txt = txt;
		}
		
		@Override
		public String toString() {
			return txt;
		}

		@Override
		public void prettyPrint(String prefix, PrintWriter writer) {
			writer.print(prefix + toString());
		}
		
	}
	
	protected Operator operator = Operator.NONE;
	public Operator getOperator() {
		return operator;
	}


	protected List<Clause> clauses = new LinkedList<>();
	
	public BooleanQuery(Operator operator, Occur occur) {
		super(occur);
		this.operator = operator;
	}
	
	public void addClause(Clause clause) {
		clauses.add(clause);
	}
	
	public List<Clause> getClauses() { return clauses; }
	 

	/* (non-Javadoc)
	 * @see qp.model.Node#prettyPrint(java.lang.String)
	 */
	@Override
	public void prettyPrint(String prefix, PrintWriter writer) {
		writer.print(prefix);
		writer.println(occur + "BQ: " + operator + "(");
		for (Clause clause: clauses) {
			clause.prettyPrint(prefix + prefix, writer);
		}
		writer.println(prefix + ")");
	}

}
