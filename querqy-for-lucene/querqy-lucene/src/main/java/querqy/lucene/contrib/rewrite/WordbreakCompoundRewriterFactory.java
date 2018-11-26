package querqy.lucene.contrib.rewrite;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.spell.WordBreakSpellChecker;
import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class WordbreakCompoundRewriterFactory implements RewriterFactory {

    private final Supplier<IndexReader> indexReaderSupplier;
    private final String dictionaryField;
    private final int maxChanges;
    private final int minSuggestionFreq;
    private final int maxCombineLength;
    private final int minBreakLength;

    public WordbreakCompoundRewriterFactory(Supplier<IndexReader> indexReaderSupplier,
                                            String dictionaryField,
                                            int maxChanges,
                                            int minSuggestionFreq,
                                            int maxCombineLength,
                                            int minBreakLength) {
        this.indexReaderSupplier = indexReaderSupplier;
        this.dictionaryField = dictionaryField;
        this.maxChanges = maxChanges;
        this.minSuggestionFreq = minSuggestionFreq;
        this.maxCombineLength = maxCombineLength;
        this.minBreakLength = minBreakLength;
    }

    @Override
    public QueryRewriter createRewriter(ExpandedQuery expandedQuery, Map<String, ?> map) {
        WordBreakSpellChecker wordBreakSpellChecker = new WordBreakSpellChecker();
        wordBreakSpellChecker.setMaxChanges(maxChanges);
        wordBreakSpellChecker.setMinSuggestionFrequency(minSuggestionFreq);
        wordBreakSpellChecker.setMaxCombineWordLength(maxCombineLength);
        wordBreakSpellChecker.setMinBreakWordLength(minBreakLength);
        wordBreakSpellChecker.setMaxEvaluations(100);
        return new WordbreakCompoundRewriter(wordBreakSpellChecker, indexReaderSupplier.get(), dictionaryField);
    }

    @Override
    public Set<Term> getGenerableTerms() {
        return QueryRewriter.EMPTY_GENERABLE_TERMS;
    }
}
