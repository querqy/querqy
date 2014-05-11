/**
 * 
 */
package qp.model;

import java.util.List;


/**
 * @author rene
 *
 */
public class DisjunctionMaxQuery extends SubQuery<DisjunctionMaxClause> implements BooleanClause {
	
	public DisjunctionMaxQuery(Occur occur) {
		super(occur);
	}

	public List<Term> getTerms() {
		return getClauses(Term.class);
	}
	
	
//	// TODO: Use filtering iterator
//	public Set<Term> getTerms() {
//		Set<Term> result = new HashSet<>();
//		for (DisjunctionMaxClause clause : clauses) {
//			if (clause instanceof Term) {
//				result.add((Term) clause);
//			}
//		}
//		return result;
//	}
	

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
