/**
 * 
 */
package querqy.model;

import java.io.CharArrayReader;
import java.io.Reader;


/**
 * @author rene
 *
 */
public class Term implements DisjunctionMaxClause {
	
	protected final String field;
	protected final char[] value;
	public final int start;
	public final int length;
	protected final SubQuery<?> parentQuery;
	

	
	public Term(SubQuery<?> parentQuery, char[] value, int start, int length) {
		this(parentQuery, null, value, start, length);
	}
	
	public Term(SubQuery<?> parentQuery, char[] value) {
        this(parentQuery, null, value);
    }
	
	public Term(SubQuery<?> parentQuery, String field, char[] value, int start, int length) {
		this.field = field;
		this.value = value;
		this.start = start;
		this.length = length;
		this.parentQuery = parentQuery;
	}
	
	public Term(SubQuery<?> parentQuery, String field, char[] value) {
        this.field = field;
        this.value = value;
        this.start = 0;
        this.length = value.length;
        this.parentQuery = parentQuery;
    }
	
	public SubQuery<?> getParentQuery() {
		return parentQuery;
	}
	
	public Term clone(SubQuery<?> newParent) {
		return new Term(newParent, field, value, start, length);
	}
	
	public Reader reader() {
	    return new CharArrayReader(value, start, length);
	}
	
	@Override
	public <T> T accept(NodeVisitor<T> visitor) {
		return visitor.visit(this);
	}
	
	public String getField() {
		return field;
	}
	
	public char charAt(int index) {
	    return value[start + index];
	}
	
	public int codePointAt(int index) {
	    return Character.codePointAt(value, index + start, start + length);
	}
	
	public String getValue() {
		return new String(value, start, length);
	}

	@Override
	public String toString() {
		return ((field == null) ? "*" : field) + ":" + getValue();
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
