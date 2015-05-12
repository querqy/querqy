/**
 * 
 */
package querqy.lucene.rewrite.cache;

import querqy.ComparableCharSequence;
import querqy.model.Term;

/**
 * @author rene
 *
 */
public class CacheKey {
    
    public final String fieldname;
    public final Term term;
    protected final ComparableCharSequence value;
    
    public CacheKey(String fieldname, Term term) {
        this.fieldname = fieldname;
        this.term = term;
        value = term.getValue();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((fieldname == null) ? 0 : fieldname.hashCode());
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
        CacheKey other = (CacheKey) obj;
        if (fieldname == null) {
            if (other.fieldname != null)
                return false;
        } else if (!fieldname.equals(other.fieldname))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    
}
