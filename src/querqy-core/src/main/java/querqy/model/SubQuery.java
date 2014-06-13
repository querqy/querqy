/**
 * 
 */
package querqy.model;

import java.util.LinkedList;
import java.util.List;




/**
 * @author rene
 *
 */
public abstract class SubQuery<T extends Clause> implements Node {
	
	public enum Occur {
		
		SHOULD(""), MUST("+"), MUST_NOT("-");
		
		final String txt;
		
		Occur(String txt) {
			this.txt = txt;
		}
		
		@Override
		public String toString() {
			return txt;
		}

	}
	
	public final Occur occur;
	
	public Occur getOccur() {
		return occur;
	}

	protected final SubQuery<?> parentQuery;
	
	protected final boolean generated;
	
	protected final List<T> clauses = new LinkedList<>();
	
	public SubQuery(SubQuery<?> parentQuery, boolean generated) {
		this(parentQuery, Occur.SHOULD, generated);
	}
	
	public SubQuery(SubQuery<?> parentQuery, Occur occur, boolean generated) {
		this.parentQuery = parentQuery;
		this.occur = occur;
		this.generated = generated;
	}
	
	@Override
	public boolean isGenerated() {
	    return generated;
	}
	
	// TODO: Use filtering iterator
	@SuppressWarnings("unchecked")
	public <C extends T> List<C> getClauses(Class<C> type) {
		List<C> result = new LinkedList<>();
		for (T clause: clauses) {
			if (type.equals(clause.getClass())) {
				result.add((C) clause);
			}
		}
		return result;
	}
	
	public SubQuery<?> getParentQuery() {
		return parentQuery;
	}
	
	public void addClause(T clause) {
		if (clause.getParentQuery() != this) {
			throw new IllegalArgumentException("This query is not a parent of " + clause);
		}
		clauses.add(clause);
	}
	
	public List<T> getClauses() {
		return clauses;
	}

}
