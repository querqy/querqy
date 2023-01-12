package querqy.rewrite.lookup.triemap;

import querqy.model.BooleanQuery;
import querqy.rewrite.lookup.LookupConfig;
import querqy.trie.TrieMap;


public class TrieMapLookupQueryVisitorFactory<ValueT> {

    private final TrieMap<ValueT> trieMap;
    private final LookupConfig lookupConfig;

    private TrieMapLookupQueryVisitorFactory(
            final TrieMap<ValueT> trieMap,
            final LookupConfig lookupConfig
    ) {
        this.trieMap = trieMap;
        this.lookupConfig = lookupConfig;
    }

    public TrieMapLookupQueryVisitor<ValueT> createTrieMapLookup(final BooleanQuery booleanQuery) {
        return new TrieMapLookupQueryVisitor<>(
                booleanQuery,
                lookupConfig,
                createAutomatonWrapper(),
                new TrieMapMatchCollector<>()
        );
    }

    private TrieMapSequenceLookup<ValueT> createAutomatonWrapper() {
        return new TrieMapSequenceLookup<>(trieMap, lookupConfig);
    }

    public TrieMap<ValueT> getTrieMap() {
        return trieMap;
    }

    public static <ValueT> TrieMapLookupQueryVisitorFactory<ValueT> of(final TrieMap<ValueT> trieMap, final LookupConfig lookupConfig) {
        return new TrieMapLookupQueryVisitorFactory<>(trieMap, lookupConfig);
    }

    public static <ValueT> TrieMapLookupQueryVisitorFactory<ValueT> of(final TrieMap<ValueT> trieMap) {
        return of(trieMap, LookupConfig.defaultConfig());
    }
}
