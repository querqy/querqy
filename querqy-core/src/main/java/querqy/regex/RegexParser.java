package querqy.regex;

import querqy.regex.Symbol.AlternationSymbol;
import querqy.regex.Symbol.AnyCharSymbol;
import querqy.regex.Symbol.AnyDigitSymbol;
import querqy.regex.Symbol.CharClassSymbol;
import querqy.regex.Symbol.CharSymbol;
import querqy.regex.Symbol.GroupSymbol;

import java.util.ArrayList;
import java.util.List;

public final class RegexParser {

    private String input;
    private int pos;
    private int nextGroupIndex = 1;

    public List<Symbol> parse(final String regex) {
        return parse(regex, 1);
    }

    public List<Symbol> parse(final String regex, final int nextGroupIndex) {

        this.input = regex;
        this.pos = 0;
        this.nextGroupIndex = nextGroupIndex;

        final List<Symbol> symbols = parseAlternation(false);// parseSequence(false);

        if (pos != input.length()) {
            throw error("Unexpected trailing input");
        }

        return symbols;
    }

    public int getGroupCount() {
        return nextGroupIndex - 1;
    }

    private List<Symbol> parseAlternation(boolean insideGroup) {
        final List<List<Symbol>> alternatives = new ArrayList<>();
        alternatives.add(parseSequence(insideGroup));

        while (pos < input.length() && input.charAt(pos) == '|') {
            pos++; // consume '|'
            alternatives.add(parseSequence(insideGroup));
        }

        if (alternatives.size() == 1) {
            return alternatives.getFirst();
        }

        return List.of(new AlternationSymbol(alternatives));
    }



    private List<Symbol> parseSequence(final boolean insideGroup) {
        final List<Symbol> symbols = new ArrayList<>();

        while (pos < input.length()) {
            final char c = input.charAt(pos);

            if (c == '|' || c == ')') {
                if (!insideGroup) {
                    throw error("Unmatched ')'");
                }
                return symbols;
            }

            symbols.add( parseAtom());
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

        final char c = input.charAt(pos);
        final Symbol base;

        // group
        if (c == '(') {
            pos++; // consume '('
            int groupIndex = nextGroupIndex++;
            List<Symbol> children = parseAlternation(true);//parseSequence(true);
            pos++; // consume ')'
            base =  new GroupSymbol(groupIndex, children);
        } else if (c == '.') {
            pos++;
            base = new AnyCharSymbol();
        } else if (c == '[') {
            base = parseCharClass();
        } else if (c == '\\') {
            base = parseEscaped();
        } else if (c == '+' || c == '?' || c == '{') {
            throw error("Quantifier without target");
        } else {
            pos++;
            base = new CharSymbol(c);
        }

        parseQuantifierIfAny(base);

        return base;
    }

    private Symbol parseCharClass() {
        int start = pos;
        int depth = 0;

        while (pos < input.length()) {
            final char c = input.charAt(pos++);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    break;
                }
            } else if (c == '\\') {
                pos++; // skip escaped char
            }
        }

        if (depth != 0) {
            throw new IllegalArgumentException("Unclosed character class");
        }

        final String classText = input.substring(start, pos);

        final CharacterClass cc = CharClassParser.parse(classText);
        return new CharClassSymbol(cc::matches);

    }
    private Symbol parseEscaped() {
        pos++; // skip '\'
        if (pos >= input.length()) throw error("Dangling escape");
        final char c = input.charAt(pos++);
        return switch (c) {
            case 'd' -> new AnyDigitSymbol();
            case '\\', '(', ')', '+', '?', '{', '}', '.' -> new CharSymbol(c);
            default -> throw error("Illegal escape: \\" + c);
        };
    }

    // ---------- quantifiers ----------

    private void parseQuantifierIfAny(final Symbol s) {
        if (pos >= input.length()) return;

        final char c = input.charAt(pos);

        if (c == '+') {
            pos++;
            s.setQuantifier(1, Integer.MAX_VALUE);
        } else if (c == '?') {
            pos++;
            s.setQuantifier(0, 1);
        } else if (c == '{') {
            pos++;
            parseBoundedQuantifier(s);
        }
    }

    private void parseBoundedQuantifier(final Symbol s) {
        final int min = parseNumber();

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

        final int max = parseNumber();

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

