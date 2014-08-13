/**
 * 
 */
package querqy;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class ComparableCharSequenceWrapper implements ComparableCharSequence {
	
	final CharSequence sequence;
	
	public ComparableCharSequenceWrapper(CharSequence sequence) {
		this.sequence = sequence; 
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#length()
	 */
	@Override
	public int length() {
		return sequence.length();
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#charAt(int)
	 */
	@Override
	public char charAt(int index) {
		return sequence.charAt(index);
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#subSequence(int, int)
	 */
	@Override
	public CharSequence subSequence(int start, int end) {
		return sequence.subSequence(start, end);
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(CharSequence other) {
		
		for (int i = 0, len = Math.min(length(), other.length()); i < len; i++) {
			char ch1 = charAt(i);
	        char ch2 = other.charAt(i);
	        if (ch1 != ch2) {
	        	return ch1 - ch2;
	        }
	    }
	        
	    return length() - other.length();
	}

	@Override
	public int hashCode() {
		
		 final int prime = 31; 
		 
	     int result = 1;
	        
         for (int i = 0, length = length(); i < length; i++) {
        	 result = prime * result + charAt(i);
         }
	        
	     return result;
	}

	@Override
	public boolean equals(Object obj) {
		
		if (this == obj)
			return true;
	    
		if (obj == null)
	        return false;
	        
	    if (!CharSequence.class.isAssignableFrom(obj.getClass()))
	        return false;
	        
	    CharSequence other = (CharSequence) obj;
	    int length = length();
	        
	    if (length != other.length())
	    	return false;
	        
	    for (int i = 0; i < length; i++) {
	    	char ch1 = charAt(i);
	        char ch2 = other.charAt(i);
	        if (ch1 != ch2) {
	        	return false;
	        }
	    }
	        
	    return true;	

	}

	@Override
	public String toString() {
		return new StringBuilder(sequence).toString();
	}
	
	

}
