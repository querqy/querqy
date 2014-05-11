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

	protected SubQuery<?> parentQuery = null;
	
	protected final List<T> clauses = new LinkedList<>();
	
	public SubQuery() {
		this(Occur.SHOULD);
	}
	
	public SubQuery(Occur occur) {
		this.occur = occur;
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
	
	public void replaceClause(T oldClause, T newClause) {
		int idx = clauses.indexOf(oldClause);
		if (idx < 0) {
			throw new IllegalArgumentException("Clause to replace does not exist. Old " + oldClause + ", replacement " + newClause);
		}
		clauses.set(idx, newClause);
		newClause.setQuery(this);
	}
	
	public void setQuery(SubQuery<?> query) {
		this.parentQuery = query;
	}
	
	public SubQuery<?> getQuery() {
		return parentQuery;
	}
	
	public void addClause(T clause) {
		clauses.add(clause);
		clause.setQuery(this);
	}
	
	public List<T> getClauses() {
		return clauses;
	}

}
