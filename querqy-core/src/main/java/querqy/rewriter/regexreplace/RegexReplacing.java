/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2026 Querqy Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package querqy.rewriter.regexreplace;

import querqy.LowerCaseCharSequence;
import querqy.regex.MatchResult;
import querqy.regex.MatchResult.GroupMatch;
import querqy.regex.RegexMap;
import querqy.rewrite.logging.ActionLog;
import querqy.rewrite.logging.InstructionLog;
import querqy.rewrite.logging.MatchLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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

    /**
     * Caps how deeply {@link #replace(CharSequence)} may recurse into the prefix/suffix
     * surrounding a match. Without this, a query containing many regex-matchable segments
     * could recurse until the thread's stack overflows. No legitimate query comes anywhere
     * near this many replaceable segments.
     */
    private static final int MAX_REPLACEMENT_DEPTH = 256;

    /**
     * Caps how many whitespace-separated tokens a single pattern match may span when searching
     * for matches anywhere in the input (see {@link #findAllMatches(CharSequence)}). No realistic
     * regexreplace rule needs to match more than this many consecutive tokens.
     */
    private static final int MAX_MATCH_SPAN_TOKENS = 32;

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
        regexMap.put("(" + pattern + ")", Replacement.build(replacementString, addCount++));
    }

    public Optional<ReplacementResult> replace(final CharSequence input) {
        return replace(input, 0);
    }

    private Optional<ReplacementResult> replace(final CharSequence input, final int depth) {
        if (depth >= MAX_REPLACEMENT_DEPTH) {
            return Optional.empty();
        }

        final CharSequence inputSeq = ignoreCase ? new LowerCaseCharSequence(input) : input;
        final Set<MatchResult<Replacement>> all = findAllMatches(inputSeq);
        if (all.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(applyReplacement(Collections.min(all, WEIGHT_COMPARATOR), inputSeq, depth));

    }

    /**
     * Finds every pattern match anywhere in {@code input}, aligned to whitespace token boundaries
     * on both ends.
     * <p>
     * Patterns are registered as plain (unwrapped) regexes (see {@link #put(String, String)}), so
     * {@link RegexMap#getAll(CharSequence)} only reports a match when a pattern consumes a
     * candidate {@code CharSequence} <em>exactly</em>. To find matches anywhere in a longer input,
     * this tokenizes the input once and tries each pattern against growing, token-aligned
     * candidate substrings: for every token start position, it tries the substring ending at each
     * subsequent token boundary (up to {@link #MAX_MATCH_SPAN_TOKENS} tokens ahead), keeping
     * whatever matches. Positions in the resulting {@link MatchResult}s are shifted back to be
     * relative to the original {@code input}.
     */
    private Set<MatchResult<Replacement>> findAllMatches(final CharSequence input) {
        final List<int[]> tokens = tokenize(input);
        final Set<MatchResult<Replacement>> results = new HashSet<>();

        for (int startIdx = 0; startIdx < tokens.size(); startIdx++) {
            final int startOffset = tokens.get(startIdx)[0];
            final int maxEndIdx = Math.min(tokens.size(), startIdx + MAX_MATCH_SPAN_TOKENS);

            for (int endIdx = startIdx; endIdx < maxEndIdx; endIdx++) {
                final int endOffset = tokens.get(endIdx)[1];
                final CharSequence candidate = input.subSequence(startOffset, endOffset);
                for (final MatchResult<Replacement> matchResult : regexMap.getAll(candidate)) {
                    results.add(shiftPositions(matchResult, startOffset));
                }
            }
        }

        return results;
    }

    /**
     * Splits {@code input} into the [start, end) offsets of its maximal runs of non-space
     * characters, treating one or more consecutive spaces as a single separator.
     */
    private static List<int[]> tokenize(final CharSequence input) {
        final List<int[]> tokens = new ArrayList<>();
        final int len = input.length();
        int i = 0;
        while (i < len) {
            while (i < len && input.charAt(i) == ' ') {
                i++;
            }
            if (i >= len) {
                break;
            }
            final int start = i;
            while (i < len && input.charAt(i) != ' ') {
                i++;
            }
            tokens.add(new int[] {start, i});
        }
        return tokens;
    }

    private static MatchResult<Replacement> shiftPositions(final MatchResult<Replacement> matchResult,
                                                            final int offset) {
        if (offset == 0) {
            return matchResult;
        }
        final Map<Integer, GroupMatch> shifted = new HashMap<>();
        for (final Map.Entry<Integer, GroupMatch> entry : matchResult.groups().entrySet()) {
            final GroupMatch groupMatch = entry.getValue();
            shifted.put(entry.getKey(), new GroupMatch(groupMatch.match(), groupMatch.position() + offset));
        }
        return new MatchResult<>(matchResult.value(), shifted);
    }

    static Map<Integer, GroupMatch> adjustGroupIndexes(final Map<Integer, GroupMatch> groups) {
        final Map<Integer, GroupMatch> result = new HashMap<>();
        for (final Map.Entry<Integer, GroupMatch> entry: groups.entrySet()) {
            result.put(entry.getKey() - 1, entry.getValue());
        }
        return result;
    }

    protected ReplacementResult applyReplacement(final MatchResult<Replacement> matchResult, final CharSequence input) {
        return applyReplacement(matchResult, input, 0);
    }

    private ReplacementResult applyReplacement(final MatchResult<Replacement> matchResult, final CharSequence input,
                                               final int depth) {
        final Map<Integer, GroupMatch> groups = adjustGroupIndexes(matchResult.groups());
        final String replacement = matchResult.value().apply(groups);
        final GroupMatch groupMatch = groups.get(0);
        final String match = groupMatch.match().toString();

        String inputString = input.toString();

        int matchStart = groupMatch.position();
        String prefix;
        if (matchStart > 0) {
            prefix = input.toString().substring(0, matchStart).trim();
        } else {
            prefix = "";
        }
        if (!prefix.isEmpty()) {
            prefix = replace(prefix, depth + 1).map(replacementResult -> replacementResult.replacement)
                    .orElse(prefix).trim();
        }

        String result = (prefix.isEmpty() ? "" : prefix + " ") + replacement;

        int matchEnd = matchStart + match.length() + 1; // incorporate whitespace
        if (matchEnd < input.length()) {
            String suffix = inputString.substring(matchEnd);
            result += " " + replace(suffix, depth + 1).map(replacementResult -> replacementResult.replacement)
                    .orElse(suffix);
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
