package querqy.rewrite.lookup;

import querqy.model.BooleanQuery;
import querqy.rewrite.lookup.model.Match;
import querqy.trie.State;
import querqy.trie.TrieMap;

import java.util.List;


public class TrieMapLookup<ValueT> {

    private final TrieMap<ValueT> trieMap;
    private final LookupConfig lookupConfig;

    private TrieMapLookup(
            final TrieMap<ValueT> trieMap,
            final LookupConfig lookupConfig
    ) {
        this.trieMap = trieMap;
        this.lookupConfig = lookupConfig;
    }

    public List<Match<ValueT>> lookupMatches(final BooleanQuery booleanQuery) {

        final AutomatonWrapper<State<ValueT>, ValueT> collector = TrieMapAutomatonWrapper.<ValueT>builder()
                .trieMap(trieMap)
                .lookupConfig(lookupConfig)
                .build();

        final AutomatonSequenceExtractor<State<ValueT>> sequenceExtractor = AutomatonSequenceExtractor.<State<ValueT>>builder()
                .booleanQuery(booleanQuery)
                .stateExchangingCollector(collector)
                .lookupConfig(lookupConfig)
                .build();

        sequenceExtractor.extractSequences();

        return collector.getMatches();
    }

    public TrieMap<ValueT> getTrieMap() {
        return trieMap;
    }

    public static <ValueT> TrieMapLookup<ValueT> of(final TrieMap<ValueT> trieMap, final LookupConfig lookupConfig) {
        return new TrieMapLookup<>(trieMap, lookupConfig);
    }

    public static <ValueT> TrieMapLookup<ValueT> of(final TrieMap<ValueT> trieMap) {
        return of(trieMap, LookupConfig.defaultConfig());
    }
}
