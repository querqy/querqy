/**
 * 
 */
package querqy.model;

import java.io.CharArrayReader;
import java.io.Reader;

import querqy.CompoundCharSequence;
import querqy.SimpleComparableCharSequence;


/**
 * @author rene
 *
 */
public class Term implements DisjunctionMaxClause, CharSequence {
	
	protected final String field;
	protected final char[] value;
	public final int start;
	public final int length;
	public final boolean generated;
	protected final SubQuery<DisjunctionMaxClause> parentQuery;
	protected final Term rewrittenFrom;
	

	
	public Term(SubQuery<DisjunctionMaxClause> parentQuery, char[] value, int start, int length) {
		this(parentQuery, null, value, start, length);
	}
	
	public Term(SubQuery<DisjunctionMaxClause> parentQuery, char[] value) {
        this(parentQuery, null, value);
    }
	
	public Term(SubQuery<DisjunctionMaxClause> parentQuery, String field, char[] value, int start, int length) {
	    this(parentQuery, field, value, start, length, null);
	}
	
	public Term(SubQuery<DisjunctionMaxClause> parentQuery, String field, char[] value) {
	    this(parentQuery, field, value, 0, value.length);
    }
	
	public Term(SubQuery<DisjunctionMaxClause> parentQuery, char[] value, int start, int length, Term rewrittenFrom) {
	    this(parentQuery, null, value, start, length, rewrittenFrom);
	}
	    
	public Term(SubQuery<DisjunctionMaxClause> parentQuery, char[] value, Term rewrittenFrom) {
	    this(parentQuery, null, value, rewrittenFrom);
	}
	    
	public Term(SubQuery<DisjunctionMaxClause> parentQuery, String field, char[] value, int start, int length, Term rewrittenFrom) {
	    this.field = field;
	    this.value = value;
	    this.start = start;
	    this.length = length;
	    this.parentQuery = parentQuery;
	    this.rewrittenFrom = rewrittenFrom;
	    this.generated = rewrittenFrom != null;
	}
	    
	public Term(SubQuery<DisjunctionMaxClause> parentQuery, String field, char[] value, Term rewrittenFrom) {
	    this(parentQuery, field, value, 0, value.length, rewrittenFrom);
	}

	@Override
	public boolean isGenerated() {
	    return generated;
	}
	
	public SubQuery<DisjunctionMaxClause> getParentQuery() {
		return parentQuery;
	}
	
	public Term clone(SubQuery<DisjunctionMaxClause> newParent, Term rewrittenFrom) {
		return new Term(newParent, field, value, start, length, rewrittenFrom);
	}
	
	public Term getRewriteRoot() {
	    return rewrittenFrom != null ? rewrittenFrom.getRewriteRoot() : this;
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
	
	/**
	 * 
	 * @return A copy of the value chars
	 */
	public char[] getChars() {
	    char[] copy = new char[length];
	    System.arraycopy(value, start, copy, 0, length);
	    return copy;
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
		
		if (value != null) {
		    for (int i = 0; i < length; i++) {
		        result = 31 * result + value[start + i];
		    }
		}
		
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		//result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		} else {
		    if (length != other.length) {
		        return false;
		    }
		    for (int i = 0; i < length; i++) {
		        if (charAt(i) != other.charAt(i)) {
		            return false;
		        }
		    }
		}

		return true;
		
	}

    @Override
    public int length() {
        return length;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        if (start >= length) {
            throw new IndexOutOfBoundsException("start " + start + " greater than/equal to length " + length);
        }
        int len = end - start;
        if (len > length) {
            throw new IndexOutOfBoundsException("end index " + end + " beyond length " + length);
            
        }
        return new SimpleComparableCharSequence(value, this.start + start, len);
    }
    
    public CharSequence toCharSequenceWithField() {
        return (field == null) ? new SimpleComparableCharSequence(value, start, length) : new CompoundCharSequence(":", field, this);
    }

   


}
