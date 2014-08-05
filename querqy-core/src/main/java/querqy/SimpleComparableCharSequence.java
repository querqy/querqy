/**
 * 
 */
package querqy;


/**
 * @author rene
 *
 */
public class SimpleComparableCharSequence implements ComparableCharSequence {
    
    private final char[] value;
    private final int start;
    private int length;
    
    public SimpleComparableCharSequence(char[] value, int start, int length) {
        if ((start + length) > value.length) {
            throw new ArrayIndexOutOfBoundsException(start + length);
        }
        this.value = value;
        this.start = start;
        this.length = length;
    }

    /* (non-Javadoc)
     * @see java.lang.CharSequence#length()
     */
    @Override
    public int length() {
        return length;
    }

    /* (non-Javadoc)
     * @see java.lang.CharSequence#charAt(int)
     */
    @Override
    public char charAt(int index) {
        if (index >= length) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return value[start + index];
    }

    /* (non-Javadoc)
     * @see java.lang.CharSequence#subSequence(int, int)
     */
    @Override
    public CharSequence subSequence(int start, int end) {
        
        if (end > length) {
            throw new ArrayIndexOutOfBoundsException(end);
        }
        if (start < 0) {
            throw new ArrayIndexOutOfBoundsException(start);
        }
        
        return new SimpleComparableCharSequence(value, this.start + start, end - start);
    }

    @Override
    public int compareTo(CharSequence other) {
        
        for (int i = 0, pos = start, len = Math.min(length, other.length()); i < len; i++) {
            char ch1 = value[pos++];
            char ch2 = other.charAt(i);
            if (ch1 != ch2) {
                return ch1 - ch2;
            }
        }
        
        return length - other.length();
    }

    @Override
    public int hashCode() {
        
        final int prime = 31; 
        int result = 1;
        
        if (value != null) {
            for (int i = 0, j = start; i < length; i++) {
                result = prime * result + value[j++];
            }
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
        return new String(value, start, length);
    }
    

}
