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

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.CharSequence#length()
    */
   @Override
   public int length() {
      return sequence.length();
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.CharSequence#charAt(int)
    */
   @Override
   public char charAt(int index) {
      return sequence.charAt(index);
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.CharSequence#subSequence(int, int)
    */
   @Override
   public CharSequence subSequence(int start, int end) {
      return sequence.subSequence(start, end);
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.Comparable#compareTo(java.lang.Object)
    */
   @Override
   public int compareTo(CharSequence other) {
       return CharSequenceUtil.compare(this, other);
   }

   @Override
   public int hashCode() {
      return CharSequenceUtil.hashCode(this);
   }

   @Override
   public boolean equals(Object obj) {
       return CharSequenceUtil.equals(this, obj);
   }

   @Override
   public String toString() {
      return new StringBuilder(sequence).toString();
   }

}
