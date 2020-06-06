/**
 * 
 */
package querqy;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class SimpleComparableCharSequence implements ComparableCharSequence {

   final char[] value;
   final int start;
   final int length;

   public SimpleComparableCharSequence(final char[] value) {
      this.value = value;
      this.start = 0;
      this.length = value.length;
   }
   public SimpleComparableCharSequence(final char[] value, final int start, final int length) {
      if ((start + length) > value.length) {
         throw new ArrayIndexOutOfBoundsException(start + length);
      }
      this.value = value;
      this.start = start;
      this.length = length;
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.CharSequence#length()
    */
   @Override
   public int length() {
      return length;
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.CharSequence#charAt(int)
    */
   @Override
   public char charAt(final int index) {
      if (index >= length) {
         throw new ArrayIndexOutOfBoundsException(index);
      }
      return value[start + index];
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.CharSequence#subSequence(int, int)
    */
   @Override
   public ComparableCharSequence subSequence(final int start, final int end) {

      if (end > length) {
         throw new ArrayIndexOutOfBoundsException(end);
      }
      if (start < 0) {
         throw new ArrayIndexOutOfBoundsException(start);
      }

      return new SimpleComparableCharSequence(value, this.start + start, end - start);
   }

   @Override
   public int compareTo(final CharSequence other) {
       return CharSequenceUtil.compare(this, other);
   }

   @Override
   public int hashCode() {

      return CharSequenceUtil.hashCode(this);
   }

   @Override
   public boolean equals(final Object obj) {
       return CharSequenceUtil.equals(this, obj);
   }

   @Override
   public String toString() {
       return new String(value, start, length);
   }

}
