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

        final StateExchangingCollector<State<ValueT>, ValueT> collector = TrieMapStateExchangingCollector.of(
                trieMap, lookupConfig.ignoreCase());

        final StateExchangingSequenceExtractor<State<ValueT>> sequenceExtractor = StateExchangingSequenceExtractor.<State<ValueT>>builder()
                .booleanQuery(booleanQuery)
                .stateExchangingCollector(collector)
                .hasBoundaries(lookupConfig.hasBoundaries())
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
