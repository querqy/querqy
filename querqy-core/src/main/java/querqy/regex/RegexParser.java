package querqy.regex;

import querqy.regex.Symbol.AnyDigitSymbol;
import querqy.regex.Symbol.CharSymbol;
import querqy.regex.Symbol.GroupSymbol;

import java.util.ArrayList;
import java.util.List;

// TODO: pass regex to constructor so we dont't have to mess with state??
public final class RegexParser {

    private String input;
    private int pos;
    private int nextGroupIndex = 1;

    public List<Symbol> parse(final String regex) {

        this.input = regex;
        this.pos = 0;
        this.nextGroupIndex = 1;

        final List<Symbol> symbols = parseSequence(false);

        if (pos != input.length()) {
            throw error("Unexpected trailing input");
        }

        return symbols;
    }

    public int getGroupCount() {
        return nextGroupIndex - 1;
    }




    private List<Symbol> parseSequence(boolean insideGroup) {
        List<Symbol> symbols = new ArrayList<>();

        while (pos < input.length()) {
            char c = input.charAt(pos);

            if (c == ')') {
                if (!insideGroup) {
                    throw error("Unmatched ')'");
                }
                pos++; // consume ')'
                return symbols;
            }

            Symbol s = parseAtom();
            parseQuantifierIfAny(s);
            symbols.add(s);
        }

        if (insideGroup) {
            throw error("Unclosed '('");
        }

        return symbols;
    }

    private Symbol parseAtom() {
        if (pos >= input.length()) {
            throw error("Unexpected end");
        }

        char c = input.charAt(pos);

        // group
        if (c == '(') {
            pos++; // consume '('
            int groupIndex = nextGroupIndex++;
            List<Symbol> children = parseSequence(true);
            return new GroupSymbol(groupIndex, children);
        }

        // escape
        if (c == '\\') {
            pos++;
            if (pos >= input.length()) {
                throw error("Dangling escape");
            }
            char escaped = input.charAt(pos++);
            return parseEscaped(escaped);
        }

        // illegal standalone quantifiers
        if (c == '+' || c == '?' || c == '{') {
            throw error("Quantifier without target");
        }

        // literal
        pos++;
        return new CharSymbol(c);
    }

    private Symbol parseEscaped(char c) {
        return switch (c) {
            case 'd' -> new AnyDigitSymbol();
            case '\\', '(', ')', '+', '?', '{', '}' -> new CharSymbol(c);
            default -> throw error("Illegal escape: \\" + c);
        };
    }

    // ---------- quantifiers ----------

    private void parseQuantifierIfAny(final Symbol s) {
        if (pos >= input.length()) return;

        char c = input.charAt(pos);

        if (c == '+') {
            pos++;
            s.setQuantifier(1, Integer.MAX_VALUE);
            return;
        }

        if (c == '?') {
            pos++;
            s.setQuantifier(0, 1);
            return;
        }

        if (c == '{') {
            pos++;
            parseBoundedQuantifier(s);
        }
    }

    private void parseBoundedQuantifier(final Symbol s) {
        int min = parseNumber();

        if (pos >= input.length()) {
            throw error("Unclosed '{'");
        }

        if (input.charAt(pos) == '}') {
            pos++;
            s.setQuantifier(min, min);
            return;
        }

        if (input.charAt(pos) != ',') {
            throw error("Expected ',' in quantifier");
        }

        pos++; // consume ','

        if (pos < input.length() && input.charAt(pos) == '}') {
            pos++;
            s.setQuantifier(min, Integer.MAX_VALUE);
            return;
        }

        int max = parseNumber();

        if (pos >= input.length() || input.charAt(pos) != '}') {
            throw error("Unclosed '{'");
        }

        pos++; // consume '}'

        if (max < min) {
            throw error("max < min in quantifier");
        }

        s.setQuantifier(min, max);
    }

    private int parseNumber() {
        if (pos >= input.length() || !Character.isDigit(input.charAt(pos))) {
            throw error("Expected number");
        }

        int n = 0;
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
            n = n * 10 + (input.charAt(pos++) - '0');
        }
        return n;
    }

    private IllegalArgumentException error(final String msg) {
        return new IllegalArgumentException(msg + " at position " + pos);
    }
}

