package querqy.rewrite.commonrules;

import static querqy.rewrite.commonrules.LineParser.BOUNDARY;
import static querqy.rewrite.commonrules.LineParser.WILDCARD;
import static querqy.rewrite.commonrules.SimpleCommonRulesParser.COMMENT_START;

// TODO: should be refactored to be usable in a more generic manner & should not be static references
public class EscapeUtil {

    static final char ESCAPE = '\\';

    public static int indexOfComment(final String s) {
        return indexIfNotEscaped(s, COMMENT_START);
    }

    public static int indexOfWildcard(final String s) {
        return indexIfNotEscaped(s, WILDCARD);
    }

    public static boolean endsWithWildcard(final String s) {
        return endsWithSpecialChar(s, WILDCARD);
    }

    public static boolean endsWithBoundary(final String s) {
        return endsWithSpecialChar(s, BOUNDARY);
    }

    // Returns the string with any of the supported escape sequences (\" \* \# and \\) un-escaped
    public static String unescape(final String s) {
        if (s.indexOf(ESCAPE) == -1) {
            return s;
        } else {
            final char[] buf = new char[s.length()];
            boolean inEscape = false;
            int j = 0;
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                if (inEscape) {
                    switch (ch) {
                        case ESCAPE:
                        case WILDCARD:
                        case COMMENT_START:
                        case BOUNDARY:
                            buf[j++] = ch;
                            break;
                        default:
                            throw new IllegalArgumentException("Illegal escape sequence \\" + ch);
                    }
                    inEscape = false;
                } else {
                    if (ch == ESCAPE) {
                        inEscape = true;
                    } else {
                        buf[j++] = ch;
                    }
                }
            }
            return new String(buf, 0, j);
        }
    }

    // Checks whether String ends with a character that has special meaning, considering an escape sequence
    public static boolean endsWithSpecialChar(final String s, final char ch) {
        return (s.length() > 0 && s.charAt(s.length() - 1) == ch && !(s.length() > 1 && s.charAt(s.length() -2) == ESCAPE));
    }

    // Returns the first position of lookupChar in s not considering occurences where lookupChar was escaped using '\'
    private static int indexIfNotEscaped(final String s, final char lookupChar) {
        boolean inEscape = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == lookupChar && !inEscape) {
                return i;
            } else {
                inEscape = ch == ESCAPE && !inEscape;
            }
        }
        return -1;
    }
}
