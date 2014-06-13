/**
 * 
 */
package querqy.model;

import java.util.List;


/**
 * @author rene
 *
 */
public class DisjunctionMaxQuery extends SubQuery<DisjunctionMaxClause> implements BooleanClause {
	
	public DisjunctionMaxQuery(SubQuery<?> parentQuery, Occur occur, boolean generated) {
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

	
	

}
