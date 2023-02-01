package querqy.rewrite.lookup.triemap;

import querqy.CompoundCharSequence;
import querqy.model.Term;
import querqy.rewrite.lookup.LookupConfig;
import querqy.rewrite.lookup.triemap.model.TrieMapSequence;
import querqy.trie.States;
import querqy.trie.TrieMap;


public class TrieMapSequenceLookup<ValueT> {

    private final TrieMap<ValueT> trieMap;
    private final LookupConfig lookupConfig;

    TrieMapSequenceLookup(final TrieMap<ValueT> trieMap, final LookupConfig lookupConfig) {
        this.trieMap = trieMap;
        this.lookupConfig = lookupConfig;
    }

    public States<ValueT> evaluateTerm(final Term term) {
        // TODO: why with field?
        final CharSequence lookupCharSequence = createLookupCharSequence(term);
        return trieMap.get(lookupCharSequence);
    }

    public States<ValueT> evaluateNextTerm(final TrieMapSequence<ValueT> sequence, final Term term) {
        final CharSequence lookupCharSequence = new CompoundCharSequence(
                null, " ", createLookupCharSequence(term));

        return trieMap.get(lookupCharSequence, sequence.getStates().getStateForCompleteSequence());
    }

    private CharSequence createLookupCharSequence(final Term term) {
        final CharSequence value = lookupConfig.getPreprocessor().process(term);
        final String field = term.getField();
        return (field == null) ? value : new CompoundCharSequence(":", field, value);
    }

}
