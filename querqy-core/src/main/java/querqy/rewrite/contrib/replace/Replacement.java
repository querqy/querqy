package querqy.rewrite.contrib.replace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Replacement {

    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("\\$(\\d+)");

    protected final List<Symbol> symbols;

    protected Replacement(final List<Symbol> symbols) {
        this.symbols = symbols;
    }


    public static Replacement build(final String input) {
        return new Replacement(parse(input));
    }

    public String apply(final Map<Integer, CharSequence> groups) {
        return symbols.stream().map(symbol -> symbol.get(groups))
                .collect(Collectors.joining(""));
    }


    protected interface Symbol {
        CharSequence get(Map<Integer, CharSequence> groups);
    }

    protected record CharSeq(CharSequence value) implements Symbol {
        @Override
        public CharSequence get(final Map<Integer, CharSequence> groups) {
            return value;
        }
    }

    protected record Placeholder(int index) implements Symbol {

        @Override
        public CharSequence get(final Map<Integer, CharSequence> groups) {
            return groups.getOrDefault(index, "");
        }
    }

    protected static List<Symbol> parse(final String input) {
        Objects.requireNonNull(input);

        List<Symbol> result = new ArrayList<>();
        StringBuilder currentText = new StringBuilder();

        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);

            //  Escape
            if (c == '\\') {
                if (i + 1 < input.length()) {
                    currentText.append(input.charAt(i + 1));
                    i += 2;
                } else {
                    currentText.append('\\');
                    i++;
                }
                continue;
            }

            // Placeholder ${digits}
            if (c == '$' && i + 1 < input.length() && input.charAt(i + 1) == '{') {

                int startDigits = i + 2;
                int j = startDigits;

                while (j < input.length() && Character.isDigit(input.charAt(j))) {
                    j++;
                }

                if (j > startDigits && j < input.length() && input.charAt(j) == '}') {

                    // flush text
                    if (!currentText.isEmpty()) {
                        result.add(new CharSeq(currentText.toString()));
                        currentText.setLength(0);
                    }

                    int index = Integer.parseInt(input.substring(startDigits, j));
                    result.add(new Placeholder(index));

                    i = j + 1;
                    continue;
                }
            }

            currentText.append(c);
            i++;
        }

        if (!currentText.isEmpty()) {
            result.add(new CharSeq(currentText.toString()));
        }

        return result;
        /*
        Objects.requireNonNull(input, "input must not be null");

        List<Symbol> result = new ArrayList<>();
        final Matcher matcher = PLACEHOLDER_PATTERN.matcher(input);

        int lastEnd = 0;

        while (matcher.find()) {

            // Text before placeholder
            if (matcher.start() > lastEnd) {
                result.add(new CharSeq(input.substring(lastEnd, matcher.start())));
            }

            // Placeholder
            final int index = Integer.parseInt(matcher.group(1));
            result.add(new Placeholder(index));

            lastEnd = matcher.end();
        }

        // Remaining trailing text
        if (lastEnd < input.length()) {
            result.add(new CharSeq(input.substring(lastEnd)));
        }

        return result;*/
    }





}
