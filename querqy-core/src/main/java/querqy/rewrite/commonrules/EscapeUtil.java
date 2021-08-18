package querqy.rewrite.commonrules;

import static querqy.rewrite.commonrules.LineParser.BOUNDARY;
import static querqy.rewrite.commonrules.LineParser.WILDCARD;
import static querqy.rewrite.commonrules.SimpleCommonRulesParser.COMMENT_START;

public class EscapeUtil {

    static final char ESCAPE = '\\';

    public static int indexOfComment(String s) {
        return indexIfNotEscaped(s, COMMENT_START);
    }

    public static int indexOfWildcard(String s) {
        return indexIfNotEscaped(s, WILDCARD);
    }

    public static boolean endsWithWildcard(String s) {
        return endsWithSpecialChar(s, WILDCARD);
    }

    public static boolean endsWithBoundary(String s) {
        return endsWithSpecialChar(s, BOUNDARY);
    }

    // Checks whether String ends with a character that has special meaning, considering an escape sequence
    public static boolean endsWithSpecialChar(String s, char ch) {
        return (s.length() > 0 && s.charAt(s.length() - 1) == ch && !(s.length() > 1 && s.charAt(s.length() -2) == ESCAPE));
    }

    public static int indexIfNotEscaped(String s, char lookupChar) {
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

    public static String unescape(String s) {
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

}
