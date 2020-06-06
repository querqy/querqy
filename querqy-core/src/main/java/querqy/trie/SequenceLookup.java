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

public class SequenceLookup<T> {

    private static String DELIMITER = " ";

    private final TrieMap<T> trieMap;
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

    public void put(final List<? extends CharSequence> terms, final T ruleObject) {
        trieMap.put(new CompoundCharSequence(DELIMITER, lc(terms)), ruleObject);
    }

    public void putPrefix(final CharSequence term, final T ruleObject) {
        prefixTrieMap.putPrefix(lc(term), ruleObject, true);
    }

    public void putSuffix(final CharSequence term, final T ruleObject) {
        suffixTrieMap.putSuffix(lc(term), ruleObject, true);
    }

    public List<PrefixMatch<T>> findSingleTermPrefixMatches(final List<? extends CharSequence> terms) {
        final List<PrefixMatch<T>> prefixMatches = new ArrayList<>();
        final AtomicInteger lookupOffset = new AtomicInteger(0);

        terms.forEach(term -> {
            prefixTrieMap.getPrefix(lc(term)).ifPresent(
                    prefixMatch -> prefixMatches.add(prefixMatch.setLookupOffset(lookupOffset.get())));
            lookupOffset.getAndIncrement(); });

        return prefixMatches;
    }

    public List<SuffixMatch<T>> findSingleTermSuffixMatches(final List<? extends CharSequence> terms) {
        final List<SuffixMatch<T>> suffixMatches = new ArrayList<>();
        final AtomicInteger lookupOffset = new AtomicInteger(0);

        terms.forEach(term -> {
            suffixTrieMap.getBySuffix(lc(term)).ifPresent(
                    suffixMatch -> suffixMatches.add(suffixMatch.setLookupOffset(lookupOffset.get())));
            lookupOffset.getAndIncrement(); });

        return suffixMatches;
    }

    public List<ExactMatch<T>> findExactMatches(final List<? extends CharSequence> terms) {

        final List<ExactMatch<T>> exactMatches = new ArrayList<>();
        int lookupIndex = 0;

        final LinkedList<LookupState<T>> lookupStates = new LinkedList<>();

        for (final CharSequence term : lc(terms)) {
            lookupStates.add(new LookupState<>(lookupIndex, new LinkedList<>(), null));

            final int lookupStatesTempSize = lookupStates.size();
            for (int i = 0; i < lookupStatesTempSize; i++) {

                final LookupState<T> lookupState = lookupStates.removeLast();

                final State<T> subsequentLookupState = lookupState.getState() != null
                        ? trieMap.get(term, lookupState.getState()).getStateForCompleteSequence()
                        : trieMap.get(term).getStateForCompleteSequence();

                if (!subsequentLookupState.isKnown) {
                    continue;
                }

                if (subsequentLookupState.isFinal() && subsequentLookupState.value != null) {
                    exactMatches.add(new ExactMatch<>(
                            lookupState.lookupOffsetStart, lookupIndex + 1, subsequentLookupState.value));
                }

                final State<T> subsequentLookupStateNext = trieMap.get(DELIMITER, subsequentLookupState).getStateForCompleteSequence();
                if (subsequentLookupStateNext.isKnown) {
                    lookupStates.addFirst(lookupState.addTerm(term).setState(subsequentLookupStateNext));
                }
            }

            lookupIndex++;
        }

        return exactMatches;
    }

    private List<CharSequence> lc(final List<? extends CharSequence> seqList) {
        return seqList.stream().map(this::lc).collect(Collectors.toCollection(LinkedList::new));
    }

    private CharSequence lc(final CharSequence seq) {
        return ignoreCase ? new LowerCaseCharSequence(seq) : seq;
    }
}
