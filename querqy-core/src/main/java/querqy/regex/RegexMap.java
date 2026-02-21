package querqy.regex;

import querqy.regex.Symbol.CharSymbol;
import querqy.trie.States;
import querqy.trie.TrieMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class RegexMap<T> {

    protected record Prefix<T>(NFAState<T> state, int nextGroupIndex) {}

    protected Map<String, Prefix<T>> prefixes = new HashMap<>();

    protected final TrieMap<PrefixAndState<T>> trieMap = new TrieMap<>();
    protected NFAState<T> prefixlessStart = new NFAState<>();
    protected final NFAMatcher<T> matcher = new NFAMatcher<>();

    protected record PrefixSplit(String prefix, List<Symbol> tail) {}
    protected record PrefixAndState<T>(String prefix, NFAState<T> state) {}

    protected static PrefixSplit extractLiteralPrefix(final List<Symbol> symbols) {

        final StringBuilder prefix = new StringBuilder(symbols.size());
        final int len = symbols.size();

        for (int i = 0; i < len && symbols.get(i) instanceof CharSymbol ch && ch.minOccur == 1 && ch.maxOccur == 1; i++) {
            prefix.append(ch.getValue());
        }

        if (prefix.isEmpty()) {
            return new PrefixSplit("", symbols);
        } else if (prefix.length() == len) {
            return new PrefixSplit(prefix.toString(), Collections.emptyList());
        } else {
            final List<Symbol> tail = symbols.subList(prefix.length(), symbols.size());
            return new PrefixSplit(prefix.toString(), tail);
        }
    }

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
        put(pattern, value, null);
    }
    public void put(final String pattern, T value, final String prefix) {

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

//        final PrefixSplit prefixSplit = extractLiteralPrefix(ast);
//        final List<Symbol> tail = prefixSplit.tail;
//
//        if (tail.isEmpty()) {
//            // all literals
//            final PrefixAndState<T> pas = getOrCreatePrefixAndState(patternString);
//            pas.state.accepting.add(new RegexEntry<>(value, parser.getGroupCount()));
//        } else {

            final NFACompiler<T> compiler = new NFACompiler<>();
            final NFAFragment<T> nfaFragment = compiler.compileSequence(ast);
//            for (final NFAState<T> start: starts) {
//                final NFAState<T> start = prefixSplit.prefix.isEmpty()
//                        ? prefixlessStart
//                        : getOrCreatePrefixAndState(prefixSplit.prefix).state;
        if (isMergeable(nfaFragment.start)) {
            nfaFragment.start.groupStarts.forEach(start::addGroupStart);
            nfaFragment.start.groupEnds.forEach(start::addGroupEnd);
            start.epsilonTransitions.addAll(nfaFragment.start.epsilonTransitions);
            nfaFragment.start.charTransitions.forEach(start::addCharTransitions);
            nfaFragment.start.charClassTransitions.forEach(start::addCharClassTransition);
        } else {
                start.addEpsilon(nfaFragment.start);

//            }

        }

        // FIXME: we might miss the following if there was only one state in the fragment and it got merged
        for (final NFAState<T> as: nfaFragment.accepts) {
            as.accepting.add(new RegexEntry<>(value, parser.getGroupCount()));
        }
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
        final Set<MatchResult<T>> result = matcher.matchAll(prefixlessStart, input, 0);
        for (final PrefixAndState<T> pas : trieMap.collectPartialMatchValues(input)) {
            result.addAll(matcher.matchAll(pas.state, input, pas.prefix.length()));
        }
        return result;
    }

    protected PrefixAndState<T> getOrCreatePrefixAndState(final String prefix) {
        PrefixAndState<T> pas = trieMap.get(prefix).getStateForCompleteSequence().value;
        if (pas == null) {
            pas = new PrefixAndState<>(prefix, new NFAState<>());
            trieMap.put(prefix, pas);
        }
        return pas;
    }

    protected static <T> void adjustGroupIndex(final NFAFragment<T> fragment, final int adjustment) {
        Queue<NFAState<T>> queue = new ArrayDeque<>();

        queue.add(fragment.start);
        Set<NFAState<T>> seen = new HashSet<>();
        seen.add(fragment.start);


        while (!queue.isEmpty()) {

            NFAState<T> current = queue.poll();

            if (!current.groupStarts.isEmpty()) {
                current.groupStarts.replaceAll(gs -> new NFAState.GroupStart(gs.group() + adjustment));
            }

            if (!current.groupEnds.isEmpty()) {
                current.groupEnds.replaceAll(gs -> new NFAState.GroupEnd(gs.group() + adjustment));
            }



            for (final NFAState<T> next: current.epsilonTransitions) {
                if (!seen.contains(next)) {
                    queue.add(next);
                    seen.add(next);
                }
            }

            for (final var nexts: current.charTransitions.values()) {
                for (final NFAState<T> next: nexts) {
                    if (!seen.contains(next)) {
                        queue.add(next);
                        seen.add(next);
                    }
                }
            }

            for (final CharClassTransition<T> t: current.charClassTransitions) {
                final NFAState<T> next = t.target();
                if (!seen.contains(next)) {
                    queue.add(next);
                    seen.add(next);
                }
            }
        }

    }
}
