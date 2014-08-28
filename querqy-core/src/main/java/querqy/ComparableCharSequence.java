/**
 * 
 */
package querqy;

/**
 * A ComparableCharSequence is a CharSequence that
 * <ul>
 * <li>is comparable (following the rules in {@link String#compareTo(String)}</li>
 * <li>defines a contract for hashCode() and equals() - where two CharSequences
 * must have the same hashCode if they have the same sequence of characters, and
 * where two CharSequences are equals if they have the same sequence of
 * characters</li>
 * </ul>
 * 
 * @author rene
 *
 */
public interface ComparableCharSequence extends CharSequence, Comparable<CharSequence> {

   @Override
   public int hashCode();

   @Override
   public boolean equals(Object obj);
}
