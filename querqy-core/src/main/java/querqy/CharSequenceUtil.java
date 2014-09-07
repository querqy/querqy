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
}
