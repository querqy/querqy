package querqy.lucene.contrib.rewrite.wordbreak;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.spell.WordBreakSpellChecker;
import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.trie.TrieMap;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class WordBreakCompoundRewriterFactory extends RewriterFactory {

    // this controls behaviour of the Lucene WordBreakSpellChecker:
    // for compounds: maximum distance of leftmost and rightmost term index
    //                e.g. max_changes = 1 for A B C D will check AB BC CD,
    //                     max_changes = 2 for A B C D will check AB ABC BC BCD CD
    // for decompounds: maximum splits performed
    //                  e.g. max_changes = 1 for ABCD will check A BCD, AB CD, ABC D,
    //                       max_changes = 2 for ABCD will check A BCD, A B CD, A BC D, AB CD, AB C D, ABC D
    // as we currently only send 2-grams to WBSP for compounding only max_changes = 1 is correctly supported
    private static final int MAX_CHANGES = 1;

    private static final int MAX_EVALUATIONS = 100;

    private final Supplier<IndexReader> indexReaderSupplier;
    private final boolean lowerCaseInput;
    private final boolean alwaysAddReverseCompounds;
    private final TrieMap<Boolean> reverseCompoundTriggerWords;
    private final int maxDecompoundExpansions;
    private final boolean verifyDecompundCollation;
    final LuceneWordBreaker wordBreaker; // package visible for testing
    private final LuceneCompounder compounder;
    private final CharArraySet protectedWords;

    /**
     * @param rewriterId The id of the rewriter
     * @param indexReaderSupplier Access to an IndexReader
     * @param morphology The (de)compounding morphology to use
     * @param dictionaryField The dictionary field name
     * @param lowerCaseInput Iff true, lowercase input before matching it against the dictionary field.
     * @param minSuggestionFreq The minimum frequency of a suggestion in the dictionary field (see {@link WordBreakSpellChecker}.setMinSuggestionFrequency())
     * @param maxCombineLength The maximum length of a suggestion when combining tokens (see {@link WordBreakSpellChecker}.setMaxCombineWordLength())
     * @param minBreakLength The minimum word part length for decompounding (see {@link WordBreakSpellChecker}.setMinBreakWordLength())
     * @param reverseCompoundTriggerWords Query tokens in this list will trigger the creation of a reverse compound of the surrounding tokens.
     * @param alwaysAddReverseCompounds Iff true, reverse shingles will be added to the query
     * @param maxDecompoundExpansions The maximum number of decompounds to add to the query
     * @param verifyDecompoundCollation   Iff true, verify that all parts of the compound cooccur in dictionaryField after decompounding
     */
    public WordBreakCompoundRewriterFactory(final String rewriterId,
                                            final Supplier<IndexReader> indexReaderSupplier,
                                            final Morphology morphology,
                                            final String dictionaryField,
                                            final boolean lowerCaseInput,
                                            final int minSuggestionFreq,
                                            final int maxCombineLength,
                                            final int minBreakLength,
                                            final List<String> reverseCompoundTriggerWords,
                                            final boolean alwaysAddReverseCompounds,
                                            final int maxDecompoundExpansions,
                                            final boolean verifyDecompoundCollation,
                                            final List<String> protectedWords) {
        super(rewriterId);
        this.indexReaderSupplier = indexReaderSupplier;
        this.lowerCaseInput = lowerCaseInput;
        this.alwaysAddReverseCompounds = alwaysAddReverseCompounds;
        this.verifyDecompundCollation = verifyDecompoundCollation;
        if (maxDecompoundExpansions < 0) {
            throw new IllegalArgumentException("maxDecompoundExpansions >= 0 required. Actual value: "
                    + maxDecompoundExpansions);
        }
        this.maxDecompoundExpansions = maxDecompoundExpansions;

        this.reverseCompoundTriggerWords = new TrieMap<>();
        if (reverseCompoundTriggerWords != null) {
            if (lowerCaseInput) {
                reverseCompoundTriggerWords
                        .forEach(word -> this.reverseCompoundTriggerWords.put(word.toLowerCase(), true));
            } else {
                reverseCompoundTriggerWords.forEach(word -> this.reverseCompoundTriggerWords.put(word, true));
            }

        }

        this.protectedWords = protectedWords == null ? CharArraySet.EMPTY_SET : new CharArraySet(protectedWords, lowerCaseInput);

        final WordBreakSpellChecker spellChecker = new WordBreakSpellChecker();
        spellChecker.setMaxChanges(MAX_CHANGES);
        spellChecker.setMinSuggestionFrequency(minSuggestionFreq);
        spellChecker.setMaxCombineWordLength(maxCombineLength);
        spellChecker.setMinBreakWordLength(minBreakLength);
        spellChecker.setMaxEvaluations(100);
        compounder = new SpellCheckerCompounder(spellChecker, dictionaryField, lowerCaseInput);

        // TODO: configure weight of strategy
        wordBreaker = new MorphologicalWordBreaker(morphology, dictionaryField, lowerCaseInput, minSuggestionFreq,
                minBreakLength, MAX_EVALUATIONS);


    }

    @Override
    public QueryRewriter createRewriter(final ExpandedQuery input,
                                        final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return new WordBreakCompoundRewriter(wordBreaker, compounder, indexReaderSupplier.get(),
                lowerCaseInput, alwaysAddReverseCompounds, reverseCompoundTriggerWords, maxDecompoundExpansions,
                verifyDecompundCollation, protectedWords);
    }

    @Override
    public Set<Term> getGenerableTerms() {
        return QueryRewriter.EMPTY_GENERABLE_TERMS;
    }

    TrieMap<Boolean> getReverseCompoundTriggerWords() {
        return reverseCompoundTriggerWords;
    }

    CharArraySet getProtectedWords() {
        return protectedWords;
    }
}
