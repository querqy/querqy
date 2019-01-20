package querqy.lucene.contrib.rewrite;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.spell.WordBreakSpellChecker;
import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.trie.TrieMap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class WordBreakCompoundRewriterFactory implements RewriterFactory {

    // this controls behaviour of the Lucene WordBreakSpellChecker:
    // for compounds: maximum distance of leftmost and rightmost term index
    //                e.g. max_changes = 1 for A B C D will check AB BC CD,
    //                     max_changes = 2 for A B C D will check AB ABC BC BCD CD
    // for decompounds: maximum splits performed
    //                  e.g. max_changes = 1 for ABCD will check A BCD, AB CD, ABC D,
    //                       max_changes = 2 for ABCD will check A BCD, A B CD, A BC D, AB CD, AB C D, ABC D
    // as we currently only send 2-grams to WBSP for compounding only max_changes = 1 is correctly supported
    private static final int MAX_CHANGES = 1;

    private final Supplier<IndexReader> indexReaderSupplier;
    private final String dictionaryField;
    private final WordBreakSpellChecker spellChecker;
    private final boolean alwaysAddReverseCompounds;
    private final TrieMap<Boolean> reverseCompoundTriggerWords;

    public WordBreakCompoundRewriterFactory(final Supplier<IndexReader> indexReaderSupplier,
                                            final String dictionaryField,
                                            final int minSuggestionFreq,
                                            final int maxCombineLength,
                                            final int minBreakLength,
                                            final List<String> reverseCompoundTriggerWords,
                                            final boolean alwaysAddReverseCompounds) {

        this.indexReaderSupplier = indexReaderSupplier;
        this.dictionaryField = dictionaryField;
        this.alwaysAddReverseCompounds = alwaysAddReverseCompounds;

        this.reverseCompoundTriggerWords = new TrieMap<>();
        if (reverseCompoundTriggerWords != null) {
            reverseCompoundTriggerWords.forEach(word -> this.reverseCompoundTriggerWords.put(word, true));
        }

        spellChecker = new WordBreakSpellChecker();
        spellChecker.setMaxChanges(MAX_CHANGES);
        spellChecker.setMinSuggestionFrequency(minSuggestionFreq);
        spellChecker.setMaxCombineWordLength(maxCombineLength);
        spellChecker.setMinBreakWordLength(minBreakLength);
        spellChecker.setMaxEvaluations(100);
    }

    @Override
    public QueryRewriter createRewriter(final ExpandedQuery expandedQuery, final Map<String, ?> context) {
        return new WordBreakCompoundRewriter(spellChecker, indexReaderSupplier.get(), dictionaryField,
                alwaysAddReverseCompounds, reverseCompoundTriggerWords);
    }

    @Override
    public Set<Term> getGenerableTerms() {
        return QueryRewriter.EMPTY_GENERABLE_TERMS;
    }
}
