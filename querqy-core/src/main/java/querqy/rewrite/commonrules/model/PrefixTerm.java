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
     * @param start
     * @param length
     * @param fieldNames
     */
    public PrefixTerm(char[] value, int start, int length, List<String> fieldNames) {
        super(value, start, length, fieldNames);
    }

    
    public boolean isPrefixOf(CharSequence other) {
        for (int i = 0, pos = start, len = Math.min(length, other.length()); i < len; i++) {
            char ch1 = value[pos++];
            char ch2 = other.charAt(i);
            if (ch1 != ch2) {
                return false;
            }
        }
        return length <= other.length();
    }
    
    @Override
    public Term findFirstMatch(Collection<? extends Term> haystack) {

        for (Term h : haystack) {

            if (isPrefixOf(h)) {

                if (fieldNames == h.fieldNames) {
                    return h;
                } else {
                    if (h.fieldNames != null && fieldNames != null) {
                        for (String name : fieldNames) {
                            if (h.fieldNames.contains(name)) {
                                return h;
                            }
                        }
                    }
                }
            }

        }

        return null;

    }
}
