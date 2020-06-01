package querqy.trie;

import querqy.trie.model.PrefixMatch;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static querqy.trie.LookupUtils.COMPARE_STATE_BY_INDEX_DESC;

public class PrefixTrieMap<T> {

    private final TrieMap<T> trieMap;

    public PrefixTrieMap() {
        this.trieMap = new TrieMap<>();
    }

    public void putPrefix(final CharSequence seq, final T value) {
        this.putPrefix(seq, value, false);
    }

    public void putPrefix(final CharSequence seq, final T value, boolean includeExactMatch) {
        if (seq.length() == 0) {
            throw new IllegalArgumentException("Must not put empty sequence into trie");
        }

        trieMap.putPrefix(seq, value);

        if (includeExactMatch) {
            trieMap.put(seq, value);
        }
    }

    public Optional<PrefixMatch<T>> getPrefix(final CharSequence seq) {
        if (seq.length() == 0) {
            return Optional.empty();
        }

        final States<T> states = trieMap.get(seq);

        final State<T> fullMatch = states.getStateForCompleteSequence();
        if (fullMatch.isFinal()) {
            return Optional.of(new PrefixMatch<>(fullMatch.index + 1, fullMatch.value));
        }

        final List<State<T>> prefixMatches = states.getPrefixes();
        if (prefixMatches != null && !prefixMatches.isEmpty()) {
            final State<T> prefixMaxMatch = Collections.max(states.getPrefixes(), COMPARE_STATE_BY_INDEX_DESC);

            final int exclusiveEnd = prefixMaxMatch.index + 1;

            return Optional.of(new PrefixMatch<>(
                    exclusiveEnd,
                    seq.subSequence(exclusiveEnd, seq.length()),
                    prefixMaxMatch.value));
        }

        return Optional.empty();
    }
}