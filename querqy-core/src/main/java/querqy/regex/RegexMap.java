package querqy.regex;

import querqy.regex.Symbol.CharSymbol;
import querqy.trie.States;
import querqy.trie.TrieMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RegexMap<T> {

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

        String patternString = replaceExactlyOnceQuantifier(pattern);
        // patternString = "([^ ]+ ){0,}" + patternString + "( [^ ]+){0,}";

        final RegexParser parser = new RegexParser();
        final List<Symbol> ast = parser.parse(patternString);
        final PrefixSplit prefixSplit = extractLiteralPrefix(ast);
        final List<Symbol> tail = prefixSplit.tail;

        if (tail.isEmpty()) {
            // all literals
            final PrefixAndState<T> pas = getOrCreatePrefixAndState(patternString);
            pas.state.accepting.add(new RegexEntry<>(value, parser.getGroupCount()));
        } else {
            final NFACompiler<T> compiler = new NFACompiler<>();
            final NFAFragment<T> nfaFragment = compiler.compileSequence(tail);
            final NFAState<T> start = prefixSplit.prefix.isEmpty()
                    ? prefixlessStart
                    : getOrCreatePrefixAndState(prefixSplit.prefix).state;
            start.addEpsilon(nfaFragment.start);
            for (final NFAState<T> as: nfaFragment.accepts) {
                as.accepting.add(new RegexEntry<>(value, parser.getGroupCount()));
            }

        }
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
}
