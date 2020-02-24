/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.Collection;
import java.util.List;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class PrefixTerm extends Term {

    /**
     * 
     * @param value The prefix (just the prefix characters, without any prefix marker)
     * @param start The offset into the value array
     * @param length The number of characters in the prefix
     * @param fieldNames The field names of this term
     */
    public PrefixTerm(char[] value, int start, int length, List<String> fieldNames) {
        super(value, start, length, fieldNames);
    }

    
    public boolean isPrefixOfCharSequence(final CharSequence other) {
        for (int i = 0, pos = start, len = Math.min(length, other.length()); i < len; i++) {
            char ch1 = value[pos++];
            char ch2 = other.charAt(i);
            if (ch1 != ch2) {
                return false;
            }
        }
        return length <= other.length();
    }

    public boolean isPrefixOf(final Term other) {

        if (isPrefixOfCharSequence(other)) {

            if (fieldNames == other.fieldNames) {
                return true;
            } else {
                if (other.fieldNames != null && fieldNames != null) {
                    for (String name : fieldNames) {
                        if (other.fieldNames.contains(name)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public boolean isPrefixOf(final querqy.model.Term other) {

        if (isPrefixOfCharSequence(other)) {

            final String otherFieldname = other.getField();

            if (fieldNames == null) {
                return true;
            } else if (otherFieldname != null && fieldNames.contains(otherFieldname)){
                return true;
            }
        }

        return false;
    }
    
    @Override
    public Term findFirstMatch(final Collection<? extends Term> haystack) {

        for (Term h : haystack) {

            if (isPrefixOf(h)) {
                return h;
            }

        }

        return null;

    }
}
