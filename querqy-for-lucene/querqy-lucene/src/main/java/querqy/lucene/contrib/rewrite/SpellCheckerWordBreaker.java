package querqy.lucene.contrib.rewrite;

import static querqy.lucene.LuceneQueryUtil.toLuceneTerm;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spell.SuggestMode;
import org.apache.lucene.search.spell.SuggestWord;
import org.apache.lucene.search.spell.WordBreakSpellChecker;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SpellCheckerWordBreaker implements LuceneWordBreaker {

    private final WordBreakSpellChecker spellChecker;
    private final String dictionaryField;
    private final boolean lowerCaseInput;

    public SpellCheckerWordBreaker(final WordBreakSpellChecker spellChecker, final String dictionaryField,
                                   final boolean lowerCaseInput) {
        this.spellChecker = spellChecker;
        this.dictionaryField = dictionaryField;
        this.lowerCaseInput = lowerCaseInput;
    }

    @Override
    public List<CharSequence[]> breakWord(final CharSequence word, final IndexReader indexReader,
                                          final int maxDecompoundExpansions, final boolean verifyCollation)
            throws IOException {

        final int decompoundsToQuery = verifyCollation ? maxDecompoundExpansions * 4 : maxDecompoundExpansions;

        final SuggestWord[][] rawSuggestions = spellChecker
                .suggestWordBreaks(toLuceneTerm(dictionaryField, word, lowerCaseInput), decompoundsToQuery, indexReader,
                        SuggestMode.SUGGEST_ALWAYS,
                        WordBreakSpellChecker.BreakSuggestionSortMethod.NUM_CHANGES_THEN_MAX_FREQUENCY);

        if (rawSuggestions.length == 0) {
            return Collections.emptyList();
        }

        if (!verifyCollation) {
            return Arrays.stream(rawSuggestions)
                    .filter(suggestion -> suggestion != null && suggestion.length > 1)
                    .limit(maxDecompoundExpansions)
                    .map(suggestWords ->
                        Arrays.stream(suggestWords).map(suggestWord -> suggestWord.string).toArray(CharSequence[]::new)
                    )
                    .collect(Collectors.toList());
        }

        final IndexSearcher searcher = new IndexSearcher(indexReader);
        return Arrays.stream(rawSuggestions)
                .filter(suggestion -> suggestion != null && suggestion.length > 1)
                .map(suggestion -> new WordBreakCompoundRewriter.MaxSortable<>(suggestion,
                        countCollatedMatches(suggestion, searcher)))
                .filter(sortable -> sortable.count > 0)
                .sorted()
                .limit(maxDecompoundExpansions) // TODO: use PriorityQueue
                .map(sortable -> sortable.obj)
                .map(suggestWords ->
                        Arrays.stream(suggestWords).map(suggestWord -> suggestWord.string).toArray(CharSequence[]::new)
                )
                .collect(Collectors.toList());
    }

    protected int countCollatedMatches(final SuggestWord[] suggestion, final IndexSearcher searcher) {
        org.apache.lucene.search.BooleanQuery.Builder builder = new org.apache.lucene.search.BooleanQuery.Builder();
        for (final SuggestWord word : suggestion) {
            builder.add(new org.apache.lucene.search.BooleanClause(
                    new TermQuery(new org.apache.lucene.index.Term(dictionaryField, word.string)),
                    org.apache.lucene.search.BooleanClause.Occur.FILTER));
        }

        try {
            return searcher.count(builder.build());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

}
