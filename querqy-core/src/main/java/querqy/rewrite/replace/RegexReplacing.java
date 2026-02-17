package querqy.rewrite.replace;

import querqy.LowerCaseCharSequence;
import querqy.regex.MatchResult;
import querqy.regex.MatchResult.GroupMatch;
import querqy.regex.RegexMap;
import querqy.regex.Symbol;
import querqy.rewrite.logging.ActionLog;
import querqy.rewrite.logging.InstructionLog;
import querqy.rewrite.logging.MatchLog;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RegexReplacing {

    static final Comparator<MatchResult<Replacement>> WEIGHT_COMPARATOR = (m1, m2) -> {

        if (m1 == m2) return 0;

        final Replacement replacement1 = m1.value();
        final Replacement replacement2 = m2.value();

        int comp = Float.compare(replacement2.weight, replacement1.weight);
        if (comp == 0) {

            int len1 = replacement1.symbols.size();
            int len2 = replacement2.symbols.size();

            comp = len2 - len1; // length of symbols first

            for (int i = 0, len = Math.min(len1, len2); (i < len) && (comp == 0); i++) {
                final Replacement.Symbol symbol1 = replacement1.symbols.get(i);
                final Replacement.Symbol symbol2 = replacement2.symbols.get(i);
                comp = switch (symbol1) {
                    case Replacement.CharSeq cs1 -> switch (symbol2) {
                            case Replacement.CharSeq cs2 -> cs2.value().length() - cs1.value().length();
                            case Replacement.Placeholder ignored -> -1;
                        };
                    case Replacement.Placeholder ps1 -> switch (symbol2) {
                        case Replacement.CharSeq ignored -> 1;
                        case Replacement.Placeholder ps2 -> Integer.compare(ps1.index(), ps2.index());
                    };
                };

            }
        }
        return comp;
    };

    public record ReplacementResult(CharSequence input, String replacement) {};

    private final RegexMap<Replacement> regexMap = new RegexMap<>();
    private final boolean ignoreCase;
    private final List<ActionLog> actionLogs;
    private int addCount = 0;

    public RegexReplacing(final boolean ignoreCase, final List<ActionLog> actionLogs) {
        this.ignoreCase = ignoreCase;
        this.actionLogs = actionLogs;
    }

    public RegexReplacing() {
        this(true, null);
    }

    public void put(final String pattern, final String replacement) {
        final String replacementString = ignoreCase ? replacement.trim().toLowerCase() : replacement.trim();
        regexMap.put("([^ ]+ ){0,}(" + pattern + ")( [^ ]+){0,}", Replacement.build(replacementString, addCount++));
    }

    public Optional<ReplacementResult> replace(final CharSequence input) {
        final CharSequence inputSeq = ignoreCase ? new LowerCaseCharSequence(input) : input;
        final Set<MatchResult<Replacement>> all = regexMap.getAll(inputSeq);
        if (all.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(applyReplacement(Collections.min(all, WEIGHT_COMPARATOR), inputSeq));

    }

    static Map<Integer, GroupMatch> adjustGroupIndexes(final Map<Integer, GroupMatch> groups) {
        final Map<Integer, GroupMatch> result = new HashMap<>();
        for (final Map.Entry<Integer, GroupMatch> entry: groups.entrySet()) {
            result.put(entry.getKey() - 2, entry.getValue());
        }
        return result;
    }

    protected ReplacementResult applyReplacement(final MatchResult<Replacement> matchResult, final CharSequence input) {
        final Map<Integer, GroupMatch> groups = adjustGroupIndexes(matchResult.groups());
        final String replacement = matchResult.value().apply(groups);
        final GroupMatch groupMatch = groups.get(0);
        final String match = groupMatch.match().toString();
//        if (match.equals(replacement)) {
//            return new ReplacementResult(input, replacement);
//        }


        String inputString = input.toString();

        int matchStart = groupMatch.position();// inputString.indexOf(match) ;
        String prefix;
        if (matchStart > 0) {
            prefix = input.toString().substring(0, matchStart).trim();
        } else {
            prefix = "";
        }
        if (!prefix.isEmpty()) {
            prefix = replace(prefix).map(replacementResult -> replacementResult.replacement).orElse(prefix).trim();
        }

        String result = (prefix.isEmpty() ? "" : prefix + " ") + replacement;

        int matchEnd = matchStart + match.length() + 1; // incorporate whitespace
        if (matchEnd < input.length()) {
            String suffix = inputString.substring(matchEnd);
            result += " " + replace(suffix).map(replacementResult -> replacementResult.replacement ).orElse(suffix);
        }

        if (actionLogs != null) {
            actionLogs.add(
                    ActionLog.builder()
                            .message(String.format("%s => %s", input, replacement))
                            .match(
                                    MatchLog.builder()
                                            .type(MatchLog.MatchType.REGEX)
                                            .term(match)
                                            .build()
                            )
                            .instructions(List.of(
                                    InstructionLog.builder()
                                            .type("replace")
                                            .value(replacement)
                                            .build()
                            ))
                            .build()
            );
        }

        return new ReplacementResult(input, result);

    }

}
