package querqy.lucene.contrib.rewrite;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.spell.WordBreakSpellChecker;
import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.trie.TrieMap;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class WordBreakCompoundRewriterFactory implements RewriterFactory {

    private final Supplier<IndexReader> indexReaderSupplier;
    private final String dictionaryField;
    private final WordBreakSpellChecker spellChecker;
    private final boolean alwaysAddReverseCompounds;
    private final TrieMap<Boolean> reverseCompoundTriggerWords;

    public WordBreakCompoundRewriterFactory(final Supplier<IndexReader> indexReaderSupplier,
                                            final String dictionaryField,
                                            final int maxChanges,
                                            final int minSuggestionFreq,
                                            final int maxCombineLength,
                                            final int minBreakLength,
                                            final boolean alwaysAddReverseCompounds,
                                            final Set<String> reverseCompoundTriggerWords) {

        this.indexReaderSupplier = indexReaderSupplier;
        this.dictionaryField = dictionaryField;
        this.alwaysAddReverseCompounds = alwaysAddReverseCompounds;

        this.reverseCompoundTriggerWords = new TrieMap<>();
        if (reverseCompoundTriggerWords != null) {
            reverseCompoundTriggerWords.forEach(word -> this.reverseCompoundTriggerWords.put(word, true));
        }

        spellChecker = new WordBreakSpellChecker();
        spellChecker.setMaxChanges(maxChanges);
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
