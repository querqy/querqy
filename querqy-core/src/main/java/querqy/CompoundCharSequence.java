/**
 * 
 */
package querqy;

import java.util.List;

/**
 * 
 * @author rene
 *
 */
public class CompoundCharSequence implements ComparableCharSequence {

   final CharSequence[] parts;

   public CompoundCharSequence(CharSequence separator, List<? extends CharSequence> parts) {
      this(separator, parts.toArray(new CharSequence[parts.size()]));
   }

   /**
    * 
    * @param separator
    * @param parts
    *           The parts to combine.
    */
   public CompoundCharSequence(CharSequence separator, CharSequence... parts) {
      if (parts == null || parts.length == 0) {
         throw new IllegalArgumentException("Excpectinig one or more parts");
      }
      if (parts.length == 1 || separator == null || separator.length() == 0) {
         this.parts = parts;
      } else {
         this.parts = new CharSequence[parts.length * 2 - 1];

         for (int i = 0, len = parts.length; i < len; i++) {
            int pos = i * 2;
            this.parts[pos] = parts[i];
            if (i < len - 1) {
               this.parts[pos + 1] = separator;
            }
         }
      }

   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.CharSequence#length()
    */
   @Override
   public int length() {

      int length = parts[0].length();

      if (parts.length > 1) {

         for (int i = 1; i < parts.length; i++) {
            length += parts[i].length();
         }

      }

      return length;

   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.CharSequence#charAt(int)
    */
   @Override
   public char charAt(int index) {

      if (parts.length == 1) {
         return parts[0].charAt(index);
      }

      PartInfo partInfo = getPartInfoForCharIndex(index);
      return parts[partInfo.partIndex].charAt(index - partInfo.globalStart);

   }

   PartInfo getPartInfoForCharIndex(int index) {
      int globalEnd = 0;
      for (int i = 0, last = parts.length - 1; i <= last; i++) {

         int globalStart = globalEnd;
         globalEnd = globalStart + parts[i].length();
         if (globalEnd > index) {
            return new PartInfo(i, globalStart);
         }

      }
      throw new ArrayIndexOutOfBoundsException(index);
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.CharSequence#subSequence(int, int)
    */
   @Override
   public CharSequence subSequence(int start, int end) {

      if (parts.length == 1) {
         return parts[0].subSequence(start, end);
      }

      PartInfo partInfoStart = getPartInfoForCharIndex(start);
      PartInfo partInfoEnd = getPartInfoForCharIndex(end - 1); // end is
                                                               // exclusive
      if (partInfoStart.partIndex == partInfoEnd.partIndex) {
         return parts[partInfoStart.partIndex].subSequence(start - partInfoStart.globalStart, end
               - partInfoStart.globalStart);
      }

      CharSequence[] resParts = new CharSequence[partInfoEnd.partIndex - partInfoStart.partIndex + 1];
      resParts[0] = parts[partInfoStart.partIndex].subSequence(start - partInfoStart.globalStart,
            parts[partInfoStart.partIndex].length());
      for (int i = partInfoStart.partIndex + 1, j = 1; i < partInfoEnd.partIndex; i++) {
         resParts[j++] = parts[i];
      }
      resParts[resParts.length - 1] = parts[partInfoEnd.partIndex].subSequence(0, end - partInfoEnd.globalStart);

      return new CompoundCharSequence(null, resParts);
   }

   class PartInfo {
      int partIndex;
      int globalStart;

      public PartInfo(int partIndex, int globalStart) {
         this.partIndex = partIndex;
         this.globalStart = globalStart;
      }
   }

   @Override
   public int compareTo(CharSequence other) {

      // TODO: avoid calls to this.charAt(i) to make comparison faster
      int length = length();
      for (int i = 0, len = Math.min(length, other.length()); i < len; i++) {
         char ch1 = charAt(i);
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

      int length = length();
      for (int i = 0; i < length; i++) {
         result = prime * result + charAt(i);
      }

      return result;
   }

   @Override
   public boolean equals(Object obj) {
       return CharSequenceUtil.equals(this, obj);
   }

   @Override
   public String toString() {
      StringBuilder buf = new StringBuilder();
      for (int i = 0; i < parts.length; i++) {
         buf.append(parts[i].toString());

      }
      return buf.toString();
   }

}
