package querqy.regex;

import querqy.regex.CharacterClass.Range;

public class CharClassParser {

    private final String input;
    private int pos;

    private CharClassParser(String input) {
        this.input = input;
        this.pos = 0;
    }

    public static CharacterClass parse(String pattern) {
        if (pattern.charAt(0) != '[') {
            throw new IllegalArgumentException("Character class must start with '['");
        }
        CharClassParser p = new CharClassParser(pattern);
        return p.parseClass();
    }

    private CharacterClass parseClass() {
        expect('[');

        CharacterClass cc = new CharacterClass();

        if (peek() == '^') {
            cc.negated = true;
            pos++;
        }

        parseUnion(cc);

        while (peek() == '&' && peek(1) == '&') {
            pos += 2; // consume &&
            CharacterClass rhs = parseClass();
            cc.intersections.add(rhs);
        }

        expect(']');
        return cc;
    }

    private void parseUnion(CharacterClass cc) {
        while (true) {
            char c = peek();
            if (c == ']' || (c == '&' && peek(1) == '&')) {
                return;
            }

            char first = parseLiteral();

            if (peek() == '-' && peek(1) != ']' ) {
                pos++; // consume '-'
                char second = parseLiteral();
                if (second < first) {
                    throw new IllegalArgumentException("Invalid range: " + first + "-" + second);
                }
                cc.ranges.add(new Range(first, second));
            } else {
                cc.singles.add(first);
            }
        }
    }

    private char parseLiteral() {
        char c = peek();

        if (c == '\\') {
            pos++;
            char escaped = peek();
            pos++;
            return escaped;
        }

        pos++;
        return c;
    }

    private char peek() {
        return pos < input.length() ? input.charAt(pos) : '\0';
    }

    private char peek(int offset) {
        int p = pos + offset;
        return p < input.length() ? input.charAt(p) : '\0';
    }

    private void expect(char c) {
        if (peek() != c) {
            throw new IllegalArgumentException(
                    "Expected '" + c + "' at position " + pos
            );
        }
        pos++;
    }
}






