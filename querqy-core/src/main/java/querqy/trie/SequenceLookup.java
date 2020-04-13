package querqy.trie;

import querqy.CompoundCharSequence;
import querqy.LowerCaseCharSequence;
import querqy.trie.model.ExactMatch;
import querqy.trie.model.LookupState;
import querqy.trie.model.PrefixMatch;
import querqy.trie.model.SuffixMatch;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SequenceLookup<T, U> {

    private static String DELIMITER = " ";

    private final TrieMap<U> trieMap;
    private final PrefixTrieMap<T> prefixTrieMap;
    private final SuffixTrieMap<T> suffixTrieMap;

    private final boolean ignoreCase;

    public SequenceLookup() {
        this(true);
    }

    public SequenceLookup(final boolean ignoreCase) {
        this.trieMap = new TrieMap<>();
        this.prefixTrieMap = new PrefixTrieMap<>();
        this.suffixTrieMap = new SuffixTrieMap<>();
        this.ignoreCase = ignoreCase;
    }

    public void put(final List<? extends CharSequence> terms, final U ruleObject) {
        trieMap.put(new CompoundCharSequence(DELIMITER, lc(terms)), ruleObject);
    }

    public void putPrefix(final CharSequence term, final T ruleObject) {
        prefixTrieMap.putPrefix(lc(term), ruleObject, true);
    }

    public void putSuffix(final CharSequence term, final T ruleObject) {
        suffixTrieMap.putSuffix(lc(term), ruleObject, true);
    }

    public List<PrefixMatch<T>> findRulesBySingleTermPrefixMatch(List<CharSequence> terms) {
        final List<PrefixMatch<T>> prefixMatches = new ArrayList<>();
        final AtomicInteger lookupOffset = new AtomicInteger(0);

        terms.forEach(term -> {
            prefixTrieMap.getPrefix(lc(term)).ifPresent(
                    prefixMatch -> prefixMatches.add(prefixMatch.setLookupOffset(lookupOffset.get())));
            lookupOffset.getAndIncrement(); });

        return prefixMatches;
    }

    public List<SuffixMatch<T>> findRulesBySingleTermSuffixMatch(List<CharSequence> terms) {
        final List<SuffixMatch<T>> suffixMatches = new ArrayList<>();
        final AtomicInteger lookupOffset = new AtomicInteger(0);

        terms.forEach(term -> {
            suffixTrieMap.getBySuffix(lc(term)).ifPresent(
                    suffixMatch -> suffixMatches.add(suffixMatch.setLookupOffset(lookupOffset.get())));
            lookupOffset.getAndIncrement(); });

        return suffixMatches;
    }

    public List<ExactMatch<U>> findRulesByExactMatch(List<CharSequence> terms) {

        final List<ExactMatch<U>> exactMatches = new ArrayList<>();
        int lookupIndex = 0;

        final LinkedList<LookupState<U>> lookupStates = new LinkedList<>();

        for (final CharSequence term : lc(terms)) {
            lookupStates.add(new LookupState<>(lookupIndex, new LinkedList<>(), null));

            final int lookupStatesTempSize = lookupStates.size();
            for (int i = 0; i < lookupStatesTempSize; i++) {

                final LookupState<U> lookupState = lookupStates.removeLast();

                final State<U> subsequentLookupState = lookupState.getState() != null
                        ? trieMap.get(term, lookupState.getState()).getStateForCompleteSequence()
                        : trieMap.get(term).getStateForCompleteSequence();

                if (!subsequentLookupState.isKnown) {
                    continue;
                }

                if (subsequentLookupState.isFinal() && subsequentLookupState.value != null) {
                    exactMatches.add(new ExactMatch<>(
                            lookupState.lookupOffsetStart, lookupIndex + 1, subsequentLookupState.value));
                }

                final State<U> subsequentLookupStateNext = trieMap.get(DELIMITER, subsequentLookupState).getStateForCompleteSequence();
                if (subsequentLookupStateNext.isKnown) {
                    lookupStates.addFirst(lookupState.addTerm(term).setState(subsequentLookupStateNext));
                }
            }

            lookupIndex++;
        }

        return exactMatches;
    }

    private List<CharSequence> lc(List<? extends CharSequence> seqList) {
        return seqList.stream().map(this::lc).collect(Collectors.toCollection(LinkedList::new));
    }

    private CharSequence lc(CharSequence seq) {
        return ignoreCase ? new LowerCaseCharSequence(seq) : seq;
    }
}
