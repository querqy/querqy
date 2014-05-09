/**
 * 
 */
package qp.model;

import java.util.LinkedList;
import java.util.List;

/**
 * @author rene
 *
 */
public class BooleanQuery extends BooleanClause {
	
	protected List<BooleanClause> clauses = new LinkedList<>();
	
	public BooleanQuery(Occur occur) {
		super(occur);
	}
	
	public void addClause(BooleanClause clause) {
		clauses.add(clause);
	}
	
	public List<BooleanClause> getClauses() {
		return clauses;
	}

	@Override
	public String toString() {
		return "BooleanQuery [occur=" + occur + ", clauses=" + clauses + "]";
	}

	@Override
	public void prettyPrint(String prefix) {
		System.out.println(prefix + "BQ " + occur + " (");
		for (BooleanClause clause: clauses) {
			clause.prettyPrint(prefix + prefix);
		}
		System.out.println(prefix + ")");
		
	}
	
	
}
