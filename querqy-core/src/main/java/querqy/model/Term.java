/**
 * 
 */
package querqy.model;

import querqy.CompoundCharSequence;
import querqy.SimpleComparableCharSequence;


/**
 * @author rene
 *
 */
public class Term implements DisjunctionMaxClause, CharSequence {
	
	protected final String field;
	protected final CharSequence value;
	protected final SubQuery<DisjunctionMaxClause> parentQuery;
	protected final boolean generated; 
	
	public Term(SubQuery<DisjunctionMaxClause> parentQuery, String field, CharSequence value, boolean generated) {
		this.field = field;
	    this.value = value;
	    this.parentQuery = parentQuery;
	    this.generated = generated;
	}
	
	public Term(SubQuery<DisjunctionMaxClause> parentQuery, String field, CharSequence value) {
		this(parentQuery, field, value, false);
	}
	
	public Term(SubQuery<DisjunctionMaxClause> parentQuery, CharSequence value) {
        this(parentQuery, null, value);
    }
	public Term(SubQuery<DisjunctionMaxClause> parentQuery, CharSequence value, boolean generated) {
		this(parentQuery, null, value, generated);
	}
	
	
	public Term(SubQuery<DisjunctionMaxClause> parentQuery, String field, char[] value, int start, int length, boolean generated) {
	    this(parentQuery, field, new SimpleComparableCharSequence(value, start, length), generated);
	}
	    
	public boolean isGenerated() {
	    return generated;
	}
	
	public SubQuery<DisjunctionMaxClause> getParentQuery() {
		return parentQuery;
	}
	
	public Term clone(SubQuery<DisjunctionMaxClause> newParent) {
		return new Term(newParent, field, value, generated);
	}
	
	@Override
	public <T> T accept(NodeVisitor<T> visitor) {
		return visitor.visit(this);
	}
	
	public String getField() {
		return field;
	}
	
	public char charAt(int index) {
	    return value.charAt(index);
	}
	
//	public int codePointAt(int index) {
//	    return Character.codePointAt(value, index);
//	}
	
	public CharSequence getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return ((field == null) ? "*" : field) + ":" + getValue();
	}


	@Override
    public int length() {
        return value.length();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
    	return value.subSequence(start, end);
    }
    
    public CharSequence toCharSequenceWithField() {
        return (field == null) ? value : new CompoundCharSequence(":", field, this);
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
