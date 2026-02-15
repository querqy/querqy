package querqy.rewrite.replace;

import querqy.LowerCaseCharSequence;
import querqy.regex.MatchResult;
import querqy.regex.MatchResult.GroupMatch;
import querqy.regex.RegexMap;
import querqy.rewrite.logging.ActionLog;
import querqy.rewrite.logging.InstructionLog;
import querqy.rewrite.logging.MatchLog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RegexReplacing {

    public record ReplacementResult(CharSequence input, String replacement) {};

    private final RegexMap<Replacement> regexMap = new RegexMap<>();
    private final boolean ignoreCase;
    private final List<ActionLog> actionLogs;

    public RegexReplacing(final boolean ignoreCase, final List<ActionLog> actionLogs) {
        this.ignoreCase = ignoreCase;
        this.actionLogs = actionLogs;
    }

    public RegexReplacing() {
        this(true, null);
    }

    public void put(final String pattern, final String replacement) {
        final String replacementString = ignoreCase ? replacement.trim().toLowerCase() : replacement.trim();
        regexMap.put("([^ ]+ ){0,}(" + pattern + ")( [^ ]+){0,}", Replacement.build(replacementString));
    }

    public Optional<ReplacementResult> replace(final CharSequence input) {
        final CharSequence inputSeq = ignoreCase ? new LowerCaseCharSequence(input) : input;
        final Set<MatchResult<Replacement>> all = regexMap.getAll(inputSeq);
        return all.stream().findFirst().map(matchResult -> applyReplacement(matchResult, inputSeq));
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
        if (match.equals(replacement)) {
            return new ReplacementResult(input, replacement);
        }


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
