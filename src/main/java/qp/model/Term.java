/**
 * 
 */
package qp.model;


/**
 * @author rene
 *
 */
public class Term implements DisjunctionMaxClause {
	
	protected String field;
	protected String value;
	protected SubQuery<?> query = null;
	
	public Term(String value) {
		this(null, value);
	}
	
	public Term(String field, String value) {
		this.field = field;
		this.value = value;
	}
	
	public void setQuery(SubQuery<?> query) {
		this.query = query;
	}
	
	public SubQuery<?> getQuery() {
		return query;
	}
	
	public Term clone(SubQuery<?> newParent) {
		Term term = new Term(field, value);
		term.query = newParent;
		return term;
	}
	
	@Override
	public <T> T accept(NodeVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		return ((field == null) ? "*" : field) + ":" + value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		Term other = (Term) obj;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (!field.equals(other.field))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}


}
