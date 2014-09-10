/**
 * 
 */
package querqy;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class CharSequenceUtil {

    public static boolean equals(CharSequence seq1, Object obj) {
        if (seq1 == obj)
            return true;

         if (seq1 == null || obj == null)
            return false;

         if (!CharSequence.class.isAssignableFrom(obj.getClass()))
            return false;

         CharSequence seq2 = (CharSequence) obj;
         int length = seq1.length();

         if (length != seq2.length())
            return false;

         for (int i = 0; i < length; i++) {
            char ch1 = seq1.charAt(i);
            char ch2 = seq2.charAt(i);
            if (ch1 != ch2) {
               return false;
            }
         }

         return true;

    }
    
    public static int compare(CharSequence seq1, CharSequence seq2) {
        for (int i = 0, len = Math.min(seq1.length(), seq2.length()); i < len; i++) {
            char ch1 = seq1.charAt(i);
            char ch2 = seq2.charAt(i);
            if (ch1 != ch2) {
               return ch1 - ch2;
            }
         }

         return seq1.length() - seq2.length();
    }
    
    public static int hashCode(CharSequence seq) {

        final int prime = 31;
        int result = 1;

        for (int i = 0, len = seq.length(); i < len; i++) {
            result = prime * result + seq.charAt(i);
        }

        return result;
     }
}
