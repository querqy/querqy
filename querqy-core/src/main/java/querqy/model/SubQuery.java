/**
 * 
 */
package querqy.model;

import java.util.LinkedList;
import java.util.List;

/**
 * @author René Kriegler, @renekrie
 *
 */
public abstract class SubQuery<P extends Node, C extends Node> extends Clause<P> {

	protected final List<C> clauses = new LinkedList<>();
	
	public SubQuery(P parentQuery, boolean generated) {
		this(parentQuery, Occur.SHOULD, generated);
	}
	
	public SubQuery(P parentQuery, Occur occur, boolean generated) {
		super(parentQuery, occur, generated);
	}
	
	
	@SuppressWarnings("unchecked")
	public <T extends C> List<T> getClauses(Class<T> type) {
		List<T> result = new LinkedList<>();
		for (C clause: clauses) {
			if (type.equals(clause.getClass())) {
				result.add((T) clause);
			}
		}
		return result;
	}
	
	public void addClause(C clause) {
		if (clause.getParent() != this) {
			throw new IllegalArgumentException("This query is not a parent of " + clause);
		}
		clauses.add(clause);
	}
	
	public void removeClause(C clause) {
	    if (clause.getParent() != this) {
            throw new IllegalArgumentException("This query is not a parent of " + clause);
        }
	    clauses.remove(clause);
	}
	
	public List<C> getClauses() {
		return clauses;
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clauses == null) ? 0 : clauses.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SubQuery<?,?> other = (SubQuery<?,?>) obj;
        if (clauses == null) {
            if (other.clauses != null)
                return false;
        } else if (!clauses.equals(other.clauses))
            return false;
        return true;
    }

}
