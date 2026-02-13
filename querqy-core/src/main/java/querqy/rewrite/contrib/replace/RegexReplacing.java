package querqy.rewrite.contrib.replace;

import querqy.regex.MatchResult;
import querqy.regex.RegexMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RegexReplacing {

    private final RegexMap<Replacement> regexMap = new RegexMap<>();

    public void put(final String pattern, final String replacement) {
        regexMap.put("([^ ]+ ){0,}(" + pattern + ")( [^ ]+){0,}", Replacement.build(replacement.trim()));
    }

    public Optional<String> replace(final String input) {
        final Set<MatchResult<Replacement>> all = regexMap.getAll(input);
        // TODO: weight matches
        return all.stream().findFirst().map(matchResult -> applyReplacement(matchResult, input));
    }

    static Map<Integer, CharSequence> adjustGroupIndexes(final Map<Integer, CharSequence> groups) {
        final Map<Integer, CharSequence> result = new HashMap<>();
        for (final Map.Entry<Integer, CharSequence> entry: groups.entrySet()) {
            result.put(entry.getKey() - 2, entry.getValue());
        }
        return result;
    }

    protected static String applyReplacement(final MatchResult<Replacement> matchResult, final String input) {
        final Map<Integer, CharSequence> groups = adjustGroupIndexes(matchResult.groups());
        final String replacement = matchResult.value().apply(groups);
        final String match = groups.get(0).toString();
        if (match.equals(replacement)) {
            return input;
        }
        String result = input;

        for (int pos = 0; pos < result.length();) {
            int idx = result.indexOf(match, pos);
            if (idx < 0) {
                return result;
            }


            int endExcl = idx + match.length();
            boolean isReplace =  ((idx == 0) || (result.charAt(idx - 1) == ' '))
                    && ((endExcl == result.length()) || (result.charAt(endExcl) == ' '));

            if (isReplace) {
                String prefix = idx == 0 ? "" : result.substring(0, idx);
                String suffix = endExcl == result.length() ? "" : result.substring(idx + match.length());
                result = prefix + replacement + suffix;
                pos += replacement.length();
            } else {
                pos++;
            }


        }

        return result;

    }


}
