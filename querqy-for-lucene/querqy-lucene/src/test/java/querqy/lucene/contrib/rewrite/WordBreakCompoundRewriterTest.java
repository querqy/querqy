package querqy.lucene.contrib.rewrite;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;
import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.must;
import static querqy.QuerqyMatchers.term;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.spell.CombineSuggestion;
import org.apache.lucene.search.spell.SuggestWord;
import org.apache.lucene.search.spell.WordBreakSpellChecker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.trie.TrieMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(MockitoJUnitRunner.class)
public class WordBreakCompoundRewriterTest {

    @Mock
    WordBreakSpellChecker wordBreakSpellChecker;

    @Mock
    IndexReader indexReader;

    @Test
    public void testNoDecompoundForSingleToken() throws IOException {

        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] {new SuggestWord[] {}});

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(wordBreakSpellChecker, indexReader, "field1",
                false, new TrieMap<>());
        Query query = new Query();
        addTerm(query, "w1w2", false);


        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery);

        assertThat((Query) rewritten.getUserQuery(),
                bq(
                        dmq(
                                term("w1w2", false)
                        )

                )
        );

    }

    @Test
    public void testDecompoundSingleTokenIntoOneTwoTokenAlternative() throws IOException {
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] { decompoundSuggestion("w1", "w2") });

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(wordBreakSpellChecker, indexReader, "field1",
                false, new TrieMap<>());
        Query query = new Query();
        addTerm(query, "w1w2", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery);

        assertThat((Query) rewritten.getUserQuery(),
                bq(
                        dmq(
                                term("w1w2", false),
                                bq(
                                        dmq(must(), term("w1", true)),
                                        dmq(must(), term("w2", true))
                                )

                        )

                )
        );
    }

    @Test
    public void testThatGeneratedTermIsNotSplit() throws IOException {
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] { decompoundSuggestion("w1", "w2") });

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(wordBreakSpellChecker, indexReader, "field1",
                false, new TrieMap<>());
        Query query = new Query();
        addTerm(query, "w1w2", true);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery);

        assertThat((Query) rewritten.getUserQuery(),
                bq(
                        dmq(
                                term("w1w2", true)

                        )

                )
        );
    }

    @Test
    public void testThatGeneratedSecondTermIsNotCompounded() throws IOException {
        // don't de-compound
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] {new SuggestWord[] {}});

        // compound of terms at idx 0+1
        when(wordBreakSpellChecker.suggestWordCombinations(any(), anyInt(), any(), any()))
                .thenReturn(new  CombineSuggestion[] { combineSuggestion("w1w2", 0, 1) });


        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(wordBreakSpellChecker, indexReader, "field1",
                false, new TrieMap<>());
        Query query = new Query();
        addTerm(query, "w1", false);
        addTerm(query, "w2", true);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery);

        assertThat((Query) rewritten.getUserQuery(),
                bq(
                        dmq(
                                term("w1", false)

                        ),
                        dmq(
                                term("w2", true)

                        )

                )
        );
    }

    @Test
    public void testThatGeneratedFirstTermIsNotCompounded() throws IOException {
        // don't de-compound
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] {new SuggestWord[] {}});

        // compound of terms at idx 0+1
        when(wordBreakSpellChecker.suggestWordCombinations(any(), anyInt(), any(), any()))
                .thenReturn(new  CombineSuggestion[] { combineSuggestion("w1w2", 0, 1) });


        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(wordBreakSpellChecker, indexReader, "field1",
                false, new TrieMap<>());
        Query query = new Query();
        addTerm(query, "w1", true);
        addTerm(query, "w2", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery);

        assertThat((Query) rewritten.getUserQuery(),
                bq(
                        dmq(
                                term("w1", true)

                        ),
                        dmq(
                                term("w2", false)

                        )

                )
        );
    }

    @Test
    public void testThatCompoundingIfGeneratedIsMixedIn() throws IOException {
        // don't de-compound
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] {new SuggestWord[] {}});

        // compound of terms at idx 0+1
        when(wordBreakSpellChecker.suggestWordCombinations(any(), anyInt(), any(), any()))
                .thenReturn(new  CombineSuggestion[] { combineSuggestion("w1w2", 0, 1) });


        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(wordBreakSpellChecker, indexReader, "field1",
                false, new TrieMap<>());
        Query query = new Query();
        addTerm(query, "w1", false);
        addTerm(query, "w2g", true);
        addTerm(query, "w2", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery);

        assertThat((Query) rewritten.getUserQuery(),
                bq(
                        dmq(
                                term("w1", false),
                                term("w1w2", true)

                        ),
                        dmq(
                                term("w2g", true)

                        ),
                        dmq(
                                term("w2", false),
                                term("w1w2", true)

                        )


                )
        );
    }


    @Test
    public void testDecompoundSingleTokenIntoTwoTwoTokenAlternatives() throws IOException {
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] { decompoundSuggestion("w1", "w2"), decompoundSuggestion("w", "1w2") });

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(wordBreakSpellChecker, indexReader, "field1",
                false, new TrieMap<>());
        Query query = new Query();
        addTerm(query, "w1w2", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery);

        assertThat((Query) rewritten.getUserQuery(),
                bq(
                        dmq(
                                term("w1w2", false),
                                bq(
                                        dmq(must(), term("w1", true)),
                                        dmq(must(), term("w2", true))
                                ),
                                bq(
                                        dmq(must(), term("w", true)),
                                        dmq(must(), term("1w2", true))
                                )

                        )

                )
        );
    }

    @Test
    public void testCompoundTwoInputTokensOnly() throws IOException {
        // don't de-compound
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] {new SuggestWord[] {}});

        // compound of terms at idx 0+1
        when(wordBreakSpellChecker.suggestWordCombinations(any(), anyInt(), any(), any()))
                .thenReturn(new  CombineSuggestion[] { combineSuggestion("w1w2", 0, 1) });

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(wordBreakSpellChecker, indexReader, "field1",
                false, new TrieMap<>());
        Query query = new Query();
        addTerm(query, "w1", false);
        addTerm(query, "w2", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery);

        assertThat((Query) rewritten.getUserQuery(),
                bq(
                        dmq(
                                term("w1", false),
                                term("w1w2", true)
                        ),
                        dmq(
                                term("w2", false),
                                term("w1w2", true)
                        )

                )
        );
    }

    @Test
    public void testNoCompoundForTwoInputTokensOnly() throws IOException {
        // don't de-compound
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] {new SuggestWord[] {}});

        when(wordBreakSpellChecker.suggestWordCombinations(any(), anyInt(), any(), any()))
                .thenReturn(new  CombineSuggestion[] { });

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(wordBreakSpellChecker, indexReader, "field1",
                false, new TrieMap<>());
        Query query = new Query();
        addTerm(query, "w1", false);
        addTerm(query, "w2", false);


        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery);

        assertThat((Query) rewritten.getUserQuery(),
                bq(
                        dmq(
                                term("w1", false)
                        ),
                        dmq(
                                term("w2", false)
                        )

                )
        );
    }

    @Test
    public void testAlwaysAddReverseCompoundsForTwoWordInput() throws IOException {

        // don't de-compound
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] {new SuggestWord[] {}});

        Map<List<String>, CombineSuggestion[]> suggestions = new HashMap<>();
        suggestions.put(Arrays.asList("w1", "w2"), new  CombineSuggestion[] { combineSuggestion("w1w2", 0, 1) });
        suggestions.put(Arrays.asList("w2", "w1"), new  CombineSuggestion[] { combineSuggestion("w2w1", 0, 1) });
        setupWordBreakMockWithCombinations(suggestions);

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(wordBreakSpellChecker, indexReader, "field1",
                true, new TrieMap<>());
        Query query = new Query();
        addTerm(query, "w1", false);
        addTerm(query, "w2", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery);

        assertThat((Query) rewritten.getUserQuery(),
                bq(
                        dmq(
                                term("w1", false),
                                term("w1w2", true),
                                term("w2w1", true)
                        ),

                        dmq(
                                term("w2", false),
                                term("w1w2", true),
                                term("w2w1", true)
                        )

                )
        );
    }

    @Test
    public void testSingleReverseCompoundTriggerWord() throws IOException {
        TrieMap<Boolean> triggerWords = new TrieMap<>();
        triggerWords.put("trigger", true);

        // don't de-compound
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] {new SuggestWord[] {}});

        Map<List<String>, CombineSuggestion[]> suggestions = new HashMap<>();
        suggestions.put(Arrays.asList("w3", "w1"), new  CombineSuggestion[] { combineSuggestion("w3w1", 0, 1) });
        setupWordBreakMockWithCombinations(suggestions);

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(wordBreakSpellChecker, indexReader, "field1",
                false, triggerWords);
        Query query = new Query();
        addTerm(query, "w1", false);
        addTerm(query, "trigger", false);
        addTerm(query, "w3", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery);

        assertThat((Query) rewritten.getUserQuery(),
                bq(
                        dmq(
                                term("w1", false),
                                term("w3w1", true)
                        ),
                        dmq(
                                term("w3", false),
                                term("w3w1", true)
                        )

                )
        );
    }

    @Test
    public void testCompoundTriggerAffectsOnlySurroundingCompound() throws IOException {
        TrieMap<Boolean> triggerWords = new TrieMap<>();
        triggerWords.put("trigger", true);

        // don't de-compound
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] {new SuggestWord[] {}});

        Map<List<String>, CombineSuggestion[]> suggestions = new HashMap<>();
        suggestions.put(Arrays.asList("w0", "w1"), new  CombineSuggestion[] { combineSuggestion("w0w1", 0, 1) });
        suggestions.put(Arrays.asList("w3", "w1"), new  CombineSuggestion[] { combineSuggestion("w3w1", 0, 1) });
        suggestions.put(Arrays.asList("w3", "w4"), new  CombineSuggestion[] { combineSuggestion("w3w4", 0, 1) });
        setupWordBreakMockWithCombinations(suggestions);

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(wordBreakSpellChecker, indexReader, "field1",
                false, triggerWords);
        Query query = new Query();
        addTerm(query, "w0", false);
        addTerm(query, "w1", false);
        addTerm(query, "trigger", false);
        addTerm(query, "w3", false);
        addTerm(query, "w4", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery);

        assertThat((Query) rewritten.getUserQuery(),
                bq(
                        dmq(
                                term("w0", false),
                                term("w0w1", true)
                        ),
                        dmq(
                                term("w1", false),
                                term("w0w1", true),
                                term("w3w1", true)
                        ),
                        dmq(
                                term("w3", false),
                                term("w3w1", true),
                                term("w3w4", true)
                        ),
                        dmq(
                                term("w4", false),
                                term("w3w4", true)
                        )
                )
        );
    }


    private void addTerm(Query query, String value, boolean isGenerated) {
        addTerm(query, null, value, isGenerated);
    }

    private void addTerm(Query query, String field, String value, boolean isGenerated) {
        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, true);
        query.addClause(dmq);
        querqy.model.Term term = new querqy.model.Term(dmq, field, value, isGenerated);
        dmq.addClause(term);
    }

    private void setupWordBreakMockWithCombinations(Map<List<String>, CombineSuggestion[]> suggestions) throws IOException {
        when(wordBreakSpellChecker.suggestWordCombinations(any(), anyInt(), any(), any()))
                .thenAnswer((Answer<CombineSuggestion[]>) invocation -> {
                    Term[] luceneTerms = (Term[]) invocation.getArguments()[0];
                    CombineSuggestion[] combineSuggestions = suggestions.get(Arrays.stream(luceneTerms).map(Term::text).collect(Collectors.toList()));
                    return combineSuggestions == null ? new CombineSuggestion[0] : combineSuggestions;
                });
    }

    private static CombineSuggestion combineSuggestion(String combination, int... indexes) {
        return new CombineSuggestion(suggestWord(combination), indexes);
    }

    private static SuggestWord[] decompoundSuggestion(String... parts) {
        return Arrays.stream(parts).map(WordBreakCompoundRewriterTest::suggestWord).toArray(SuggestWord[]::new);
    }

    private static SuggestWord suggestWord(String suggestion) {
        SuggestWord suggestWord = new SuggestWord();
        suggestWord.string = suggestion;
        return suggestWord;
    }
}
