/**
 * 
 */
package querqy;

import java.util.List;

/**
 * A {@link ComparableCharSequence} that is composed of one or more parts (sub-sequences).
 *
 * @author René Kriegler, @renekrie
 *
 */
public class CompoundCharSequence implements ComparableCharSequence {
    
   final CharSequence[] parts;
   
   public CompoundCharSequence(final List<? extends CharSequence> parts) {
       this(null, parts);
   }

   public CompoundCharSequence(final CharSequence separator, final List<? extends CharSequence> parts) {
      this(separator, parts.toArray(new CharSequence[parts.size()]));
   }

   /**
    * 
    * @param separator A separator that is placed between the parts. Can be null.
    * @param parts The parts to combine.
    */
   public CompoundCharSequence(final CharSequence separator, final CharSequence... parts) {
      if (parts == null || parts.length == 0) {
         throw new IllegalArgumentException("Excpectinig one or more parts");
      }
      if (parts.length == 1 || separator == null || separator.length() == 0) {
         this.parts = parts;
      } else {
         this.parts = new CharSequence[parts.length * 2 - 1];

         for (int i = 0; i < parts.length; i++) {
            int pos = i * 2;
            this.parts[pos] = parts[i];
            if (i < parts.length - 1) {
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
   public char charAt(final int index) {

      if (parts.length == 1) {
         return parts[0].charAt(index);
      }

      final PartInfo partInfo = getPartInfoForCharIndex(index);
      return parts[partInfo.partIndex].charAt(index - partInfo.globalStart);

   }

   PartInfo getPartInfoForCharIndex(final int index) {
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
   public ComparableCharSequence subSequence(final int start, final int end) {

      if (parts.length == 1) {
          // TODO: do subsequence as view in wrapper
         return new ComparableCharSequenceWrapper(parts[0].subSequence(start, end));
      }

      if (start == end) {
          if (start <= length()) {
              return ComparableCharSequenceWrapper.EMPTY_SEQUENCE;
          } else {
              throw new ArrayIndexOutOfBoundsException(start);
          }
      }

      final PartInfo partInfoStart = getPartInfoForCharIndex(start);
      final PartInfo partInfoEnd = getPartInfoForCharIndex(end - 1); // end is exclusive

      if (partInfoStart.partIndex == partInfoEnd.partIndex) {
       // TODO: do subsequence as view in wrapper
         return new ComparableCharSequenceWrapper(
                 parts[partInfoStart.partIndex]
                         .subSequence(start - partInfoStart.globalStart, end - partInfoStart.globalStart));
      }

      final CharSequence[] resParts = new CharSequence[partInfoEnd.partIndex - partInfoStart.partIndex + 1];
      resParts[0] = parts[partInfoStart.partIndex]
              .subSequence(start - partInfoStart.globalStart, parts[partInfoStart.partIndex].length());

      for (int i = partInfoStart.partIndex + 1, j = 1; i < partInfoEnd.partIndex; i++) {
         resParts[j++] = parts[i];
      }
      resParts[resParts.length - 1] = parts[partInfoEnd.partIndex].subSequence(0, end - partInfoEnd.globalStart);

      return new CompoundCharSequence(null, resParts);
   }

   class PartInfo {
       final int partIndex;
       final int globalStart;

       public PartInfo(final int partIndex, final int globalStart) {
           this.partIndex = partIndex;
           this.globalStart = globalStart;
       }
   }

    @Override
    public int compareTo(final CharSequence other) {

        // TODO: avoid calls to this.charAt(i) to make comparison faster
        final int length = length();
        for (int i = 0, len = Math.min(length, other.length()); i < len; i++) {
            final char ch1 = charAt(i);
            final char ch2 = other.charAt(i);
            if (ch1 != ch2) {
                return ch1 - ch2;
            }
        }

        return length - other.length();

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
       final StringBuilder buf = new StringBuilder();
       for (int i = 0; i < parts.length; i++) {
           buf.append(parts[i].toString());

       }
       return buf.toString();
   }

}
