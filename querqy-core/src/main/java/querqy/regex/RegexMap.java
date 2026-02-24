package querqy.regex;


import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class RegexMap<T> {

    protected record Prefix<T>(NFAState<T> state, int nextGroupIndex) {}

    protected Map<String, Prefix<T>> prefixes = new HashMap<>();
    protected Map<String, NFASuffix<T>> suffixes = new HashMap<>();

    protected NFAState<T> prefixlessStart = new NFAState<>();
    protected final NFAMatcher<T> matcher = new NFAMatcher<>();

    static String replaceExactlyOnceQuantifier(final String input) {
        // FIXME: a {1} quantifier causes problems when finding char prefixes. We'd have to model the
        // quantifier as a symbol to properly solve this. Use this hack meanwhile:
        String patternString = input;
        int pos = 0;
        while ((pos = patternString.indexOf("{1}", pos)) > -1 ) {
            if ((pos == 0) || (patternString.charAt(pos - 1) != '\\')) {
                patternString = patternString.substring(0, pos) + patternString.substring(pos + 3);
            }
            pos++;
        }
        // deals with input like "{1}" or "{1}{1}{1}". These are invalid regex patterns, which we let the parser deal
        // with
        return (patternString.equals("{1}") || patternString.isEmpty()) ?  input : patternString;
    }

    public void put(final String pattern, T value) {
        put(pattern, value, null, null);
    }

    public void put(final String pattern, T value, final String prefix, final String suffix) {

        String patternString = replaceExactlyOnceQuantifier(pattern);

        final RegexParser parser = new RegexParser();

        final int nextGroupIndex;
        final NFAState<T> start;

        if (prefix == null) {
            nextGroupIndex = 1;
            start = prefixlessStart;
        } else {
            Prefix<T> compiledPrefix = prefixes.get(prefix);
            if (compiledPrefix != null) {
                nextGroupIndex = compiledPrefix.nextGroupIndex();
                start = compiledPrefix.state;
            } else {
                final List<Symbol> prefixAst = parser.parse(prefix);
                final NFACompiler<T> compiler = new NFACompiler<>();
                final NFAFragment<T> fragment = compiler.compileSequence(prefixAst);
                prefixlessStart.addEpsilon(fragment.start);
                nextGroupIndex = parser.getGroupCount() + 1;

                if (fragment.accepts.size() == 1) {
                    compiledPrefix = new Prefix<>(fragment.accepts.iterator().next(), nextGroupIndex);
                } else {
                    final NFAState<T> endOfPrefix = new NFAState<>();
                    for (final NFAState<T> accept: fragment.accepts) {
                        accept.addEpsilon(endOfPrefix);
                    }
                    compiledPrefix = new Prefix<>(endOfPrefix, nextGroupIndex);
                }

                prefixes.put(prefix, compiledPrefix);
                start = compiledPrefix.state;
            }

        }

        final List<Symbol> ast = parser.parse(patternString, nextGroupIndex);

        final NFACompiler<T> compiler = new NFACompiler<>();
        final NFAFragment<T> nfaFragment = compiler.compileSequence(ast);

        if (isMergeable(nfaFragment.start)) {
            nfaFragment.start.groupStarts.forEach(start::addGroupStart);
            nfaFragment.start.groupEnds.forEach(start::addGroupEnd);
            start.epsilonTransitions.addAll(nfaFragment.start.epsilonTransitions);
            nfaFragment.start.charTransitions.forEach(start::addCharTransitions);
            nfaFragment.start.charClassTransitions.forEach(start::addCharClassTransition);
        } else {
            start.addEpsilon(nfaFragment.start);
        }

        if (suffix != null) {
            final NFASuffix<T> nfaSuffix = getOrCreateSuffix(suffix);
            final NFAState.SuffixTransition<T> suffixTransition = new NFAState.SuffixTransition<>(nfaSuffix,
                    Set.of(new RegexEntry<>(value, nfaSuffix.suffixGroupCount() + parser.getGroupCount())),
                    parser.getGroupCount());
            for (final NFAState<T> as: nfaFragment.accepts) {
                as.addSuffixTransition(suffixTransition);
            }
        } else {
            // FIXME: we might miss the following if there was only one state in the fragment and it got merged
            for (final NFAState<T> as : nfaFragment.accepts) {
                as.accepting.add(new RegexEntry<>(value, parser.getGroupCount()));
            }
        }
    }

    NFASuffix<T> getOrCreateSuffix(final String suffixPattern) {
        NFASuffix<T> suffix = suffixes.get(suffixPattern);
        if (suffix == null) {
            final RegexParser parser = new RegexParser();
            final List<Symbol> ast = parser.parse(suffixPattern);
            final NFACompiler<T> compiler = new NFACompiler<>();
            final NFAFragment<T> nfaFragment = compiler.compileSequence(ast);
            suffix = new NFASuffix<>(nfaFragment.start, parser.getGroupCount());
            suffixes.put(suffixPattern, suffix);
        }
        return suffix;

    }

    static <T> boolean isMergeable(final NFAState<T> state) {
        final Queue<NFAState<T>> queue = new ArrayDeque<>();
        final Set<NFAState<T>> seen = new HashSet<>();
        queue.add(state);

        while (!queue.isEmpty()) {

            final NFAState<T> current = queue.poll();

            for (final NFAState<T> next: current.epsilonTransitions) {
                if (next == state) {
                    return false;
                }
                if (!seen.contains(next)) {
                    seen.add(next);
                    queue.add(next);
                }
            }

            for (final var list: current.charTransitions.values()) {
                for (final NFAState<T> next: list) {
                    if (next == state) {
                        return false;
                    }
                    if (!seen.contains(next)) {
                        seen.add(next);
                        queue.add(next);
                    }
                }
            }

            for (final CharClassTransition<T> t: current.charClassTransitions) {
                final NFAState<T> next = t.target();
                if (next == state) {
                    return false;
                }
                if (!seen.contains(next)) {
                    seen.add(next);
                    queue.add(next);
                }
            }
        }

        return true;
    }

    public Set<MatchResult<T>> getAll(final CharSequence input) {
        return matcher.matchAll(prefixlessStart, input, 0);
    }

}
