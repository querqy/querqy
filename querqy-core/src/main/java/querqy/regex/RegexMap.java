package querqy.regex;

import querqy.regex.Symbol.CharSymbol;
import querqy.trie.TrieMap;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class RegexMap<T> {

    protected final TrieMap<PrefixAndState> trieMap = new TrieMap<>();
    protected NFAState prefixlessStart = new NFAState();

    protected record PrefixSplit(String prefix, List<Symbol> tail) {}
    protected record PrefixAndState(String prefix, NFAState state) {}

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

        final RegexParser parser = new RegexParser();
        final List<Symbol> ast = parser.parse(patternString);
        final PrefixSplit prefixSplit = extractLiteralPrefix(ast);
        final List<Symbol> tail = prefixSplit.tail;

        if (tail.isEmpty()) {
            // all literals
            final PrefixAndState pas = getOrCreatePrefixAndState(patternString);
            pas.state.accepting.add(new RegexEntry<>(value, parser.getGroupCount()));
        } else {
            final NFAFragment nfaFragment = NFACompiler.compileSequence(tail);
            final NFAState start = prefixSplit.prefix.isEmpty()
                    ? prefixlessStart
                    : getOrCreatePrefixAndState(prefixSplit.prefix).state;
            start.addEpsilon(nfaFragment.start);
            for (final NFAState as: nfaFragment.accepts) {
                as.accepting.add(new RegexEntry<>(value, parser.getGroupCount()));
            }

        }
    }

    public Set<MatchResult> getAll(final String input) {
        final Set<MatchResult> result = NFAMatcher.matchAll(prefixlessStart, input, 0);
        for (final PrefixAndState pas : trieMap.collectPartialMatchValues(input)) {
            result.addAll(NFAMatcher.matchAll(pas.state, input, pas.prefix.length()));
        }
        return result;
    }

    protected PrefixAndState getOrCreatePrefixAndState(final String prefix) {
        PrefixAndState pas = trieMap.get(prefix).getStateForCompleteSequence().value;
        if (pas == null) {
            pas = new PrefixAndState(prefix, new NFAState());
            trieMap.put(prefix, pas);
        }
        return pas;
    }
}
