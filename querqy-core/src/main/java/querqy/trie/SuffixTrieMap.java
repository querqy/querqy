package querqy.trie;

import querqy.ReverseComparableCharSequence;
import querqy.trie.model.SuffixMatch;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static querqy.trie.RuleExtractorUtils.COMPARE_STATE_BY_INDEX_DESC;

public class SuffixTrieMap<T> {

    private final TrieMap<T> trieMap;

    public SuffixTrieMap() {
        this.trieMap = new TrieMap<>();
    }

    public void putSuffix(CharSequence seq, T value) {
        this.putSuffix(seq, value, false);
    }

    public void putSuffix(CharSequence seq, T value, boolean includeExactMatch) {
        if (seq.length() == 0) {
            throw new IllegalArgumentException("Must not put empty sequence into trie");
        }

        ReverseComparableCharSequence revSeq = new ReverseComparableCharSequence(seq);
        trieMap.putPrefix(revSeq, value);

        if (includeExactMatch) {
            trieMap.put(revSeq, value);
        }
    }

    public Optional<SuffixMatch<T>> getBySuffix(CharSequence seq) {
        if (seq.length() == 0) {
            return Optional.empty();
        }

        ReverseComparableCharSequence revSeq = new ReverseComparableCharSequence(seq);

        States<T> states = trieMap.get(revSeq);

        State<T> fullMatch = states.getStateForCompleteSequence();
        if (fullMatch.isFinal()) {
            return Optional.of(new SuffixMatch<>(seq.length() - (fullMatch.index + 1), fullMatch.value));
        }

        List<State<T>> suffixMatches = states.getPrefixes();
        if (suffixMatches != null && !suffixMatches.isEmpty()) {
            State<T> suffixMaxMatch = Collections.max(states.getPrefixes(), COMPARE_STATE_BY_INDEX_DESC);
            return Optional.of(new SuffixMatch<>(seq.length() - (suffixMaxMatch.index + 1), suffixMaxMatch.value));
        }

        return Optional.empty();
    }
}