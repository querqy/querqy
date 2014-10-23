/**
 * 
 */
package querqy;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class LowerCaseCharSequence implements ComparableCharSequence {

    final CharSequence delegate;
    
    public LowerCaseCharSequence(CharSequence delegate) {
        this.delegate = delegate;
    }

    @Override
    public char charAt(int index) {
        char ch = delegate.charAt(index);
        return Character.isLowerCase(ch) ? ch : Character.toLowerCase(ch);
    }
    
    @Override
    public CharSequence subSequence(int start, int end) {
        return new LowerCaseCharSequence(delegate.subSequence(start, end));
    }
    
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

    @Override
    public int length() {
        return delegate.length();
    }

    @Override
    public int compareTo(CharSequence other) {
        return CharSequenceUtil.compare(this, other);
    }
    
    @Override
    public boolean equals(Object obj) {
        return CharSequenceUtil.equals(this, obj);
    }
    
    @Override
    public int hashCode() {
        return CharSequenceUtil.hashCode(this);
    }
}
