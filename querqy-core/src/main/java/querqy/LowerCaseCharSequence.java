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
    
    public LowerCaseCharSequence(final CharSequence delegate) {
        this.delegate = delegate;
    }

    @Override
    public char charAt(final int index) {
        final char ch = delegate.charAt(index);
        return Character.isLowerCase(ch) ? ch : Character.toLowerCase(ch);
    }
    
    @Override
    public ComparableCharSequence subSequence(final int start, final int end) {
        return new LowerCaseCharSequence(delegate.subSequence(start, end));
    }
    
    @Override
    public String toString() {
        return delegate.toString().toLowerCase();
    }

    @Override
    public int length() {
        return delegate.length();
    }

    @Override
    public int compareTo(final CharSequence other) {
        return CharSequenceUtil.compare(this, other);
    }
    
    @Override
    public boolean equals(final Object obj) {
        return CharSequenceUtil.equals(this, obj);
    }
    
    @Override
    public int hashCode() {
        return CharSequenceUtil.hashCode(this);
    }
}
