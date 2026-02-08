package querqy.regex;

import querqy.regex.Symbol.AnyDigitSymbol;
import querqy.regex.Symbol.CharSymbol;
import querqy.regex.Symbol.GroupSymbol;

import java.util.ArrayList;
import java.util.List;

public final class RegexParser {

    private record ParseResult(List<Symbol> symbols, int index) { }

    public static List<Symbol> parse(final String regex) {
        ParseResult result = parseSequence(regex, 0, 0);
        if (result.index != regex.length()) {
            throw new IllegalArgumentException("Unmatched ')'");
        }
        return result.symbols;
    }

    private static ParseResult parseSequence(final String regex, final int start, final int nestingLevel) {

        final List<Symbol> symbols = new ArrayList<>();

        int i = start;

        while (i < regex.length()) {

            final char c = regex.charAt(i);

            // ----- closing group -----
            if (c == ')') {
                if (nestingLevel == 0) {
                    throw new IllegalArgumentException( "Unmatched ')' at position " + i);
                }
                return new ParseResult(symbols, i + 1);
            }

            Symbol symbol;

            // ----- opening group -----
            if (c == '(') {
                final ParseResult inner = parseSequence(regex, i + 1, nestingLevel + 1);
                symbol = new GroupSymbol(inner.symbols);
                i = inner.index - 1;
            }
            // ----- escape -----
            else if (c == '\\') {
                if (i + 1 >= regex.length()) {
                    throw new IllegalArgumentException("Dangling escape");
                }
                final char next = regex.charAt(++i);
                symbol = switch (next) {
                    case 'd' -> new AnyDigitSymbol();
                    case '\\', '{', '+', '?', '(', ')' -> new CharSymbol(next);
                    default -> throw new IllegalArgumentException("Illegal escape sequence: \\" + next);
                };
            }
            // ----- quantifier without target -----
            else if (isQuantifier(c)) {
                throw new IllegalArgumentException(
                        "Quantifier without target at position " + i
                );
            }
            // ----- literal -----
            else {
                symbol = new CharSymbol(c);
            }

            // ----- quantifier application -----
            if (i + 1 < regex.length()) {
                char q = regex.charAt(i + 1);
                if (q == '+') {
                    symbol.setOccurrence(1, Integer.MAX_VALUE);
                    i++;
                } else if (q == '?') {
                    symbol.setOccurrence(0, 1);
                    i++;
                } else if (q == '{') {
                    i = parseCount(regex, i + 1, symbol);
                }
            }

            symbols.add(symbol);
            i++;
        }

        if (nestingLevel > 0) {
            throw new IllegalArgumentException("Unclosed '('");
        }

        return new ParseResult(symbols, i);
    }

    private static boolean isQuantifier(final char c) {
        return c == '+' || c == '?' || c == '{';
    }

    private static int parseCount(final String regex, final int start, final Symbol symbol) {
        final int end = regex.indexOf('}', start);
        if (end == -1) {
            throw new IllegalArgumentException("Unclosed '{'");
        }

        final String content = regex.substring(start + 1, end);
        final String[] parts = content.split(",", -1);

        int min;
        int max;

        try {
            if (parts.length == 1) {
                min = max = Integer.parseInt(parts[0]);
            } else if (parts.length == 2) {
                min = parts[0].isEmpty() ? 0 : Integer.parseInt(parts[0]);
                max = parts[1].isEmpty()
                        ? Integer.MAX_VALUE
                        : Integer.parseInt(parts[1]);
            } else {
                throw new IllegalArgumentException("Invalid quantifier {" + content + "}");
            }
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number in {" + content + "}");
        }

        if (min > max) {
            throw new IllegalArgumentException("min > max in {" + content + "}");
        }

        symbol.setOccurrence(min, max);
        return end;
    }

}

