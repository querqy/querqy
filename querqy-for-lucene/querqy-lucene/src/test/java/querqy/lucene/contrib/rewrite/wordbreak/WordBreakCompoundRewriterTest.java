package querqy.lucene.contrib.rewrite.wordbreak;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.must;
import static querqy.QuerqyMatchers.term;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.spell.CombineSuggestion;
import org.apache.lucene.search.spell.SuggestWord;
import org.apache.lucene.search.spell.WordBreakSpellChecker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.trie.TrieMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
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

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                new SpellCheckerWordBreaker(wordBreakSpellChecker, "field1", false),
                new SpellCheckerCompounder(wordBreakSpellChecker, "field1", false),
                indexReader, false, false, new TrieMap<>(), 5, false,
                CharArraySet.EMPTY_SET);
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

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                new SpellCheckerWordBreaker(wordBreakSpellChecker, "field1", false),
                new SpellCheckerCompounder(wordBreakSpellChecker, "field1", false),
                indexReader, false, false, new TrieMap<>(), 5, false,
                CharArraySet.EMPTY_SET);
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
    public void testThatGeneratedTermIsNotSplit() {

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                new SpellCheckerWordBreaker(wordBreakSpellChecker, "field1", false),
                new SpellCheckerCompounder(wordBreakSpellChecker, "field1", false),
                indexReader, false, false, new TrieMap<>(), 5, false,
                CharArraySet.EMPTY_SET);
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
//        when(wordBreakSpellChecker.suggestWordCombinations(any(), anyInt(), any(), any()))
//                .thenReturn(new  CombineSuggestion[] { combineSuggestion("w1w2", 0, 1) });


        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                new SpellCheckerWordBreaker(wordBreakSpellChecker, "field1", false),
                new SpellCheckerCompounder(wordBreakSpellChecker, "field1", false),
                indexReader, false, false, new TrieMap<>(), 5, false,
                CharArraySet.EMPTY_SET);
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
//        when(wordBreakSpellChecker.suggestWordCombinations(any(), anyInt(), any(), any()))
//                .thenReturn(new  CombineSuggestion[] { combineSuggestion("w1w2", 0, 1) });


        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                new SpellCheckerWordBreaker(wordBreakSpellChecker, "field1", false),
                new SpellCheckerCompounder(wordBreakSpellChecker, "field1", false),
                indexReader, false, false, new TrieMap<>(), 5, false,
                CharArraySet.EMPTY_SET);
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


        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                new SpellCheckerWordBreaker(wordBreakSpellChecker, "field1", false),
                new SpellCheckerCompounder(wordBreakSpellChecker, "field1", false),
                indexReader, false, false, new TrieMap<>(), 5, false,
                CharArraySet.EMPTY_SET);
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

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                new SpellCheckerWordBreaker(wordBreakSpellChecker, "field1", false),
                new SpellCheckerCompounder(wordBreakSpellChecker, "field1", false),
                indexReader, false, false, new TrieMap<>(), 5, false,
                CharArraySet.EMPTY_SET);
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
    public void testThatOnlyMaxExpansionsAreApplied() throws IOException {
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] { decompoundSuggestion("w3", "w4"), decompoundSuggestion("w", "3w4"),
                        decompoundSuggestion("w3w", "4") });

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                new SpellCheckerWordBreaker(wordBreakSpellChecker, "field1", false),
                new SpellCheckerCompounder(wordBreakSpellChecker, "field1", false),
                indexReader, false, false, new TrieMap<>(), 2, false,
                CharArraySet.EMPTY_SET);
        Query query = new Query();
        addTerm(query, "w3w4", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery);

        assertThat((Query) rewritten.getUserQuery(),
                bq(
                        dmq(
                                term("w3w4", false),
                                bq(
                                        dmq(must(), term("w3", true)),
                                        dmq(must(), term("w4", true))
                                ),
                                bq(
                                        dmq(must(), term("w", true)),
                                        dmq(must(), term("3w4", true))
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

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                new SpellCheckerWordBreaker(wordBreakSpellChecker, "field1", false),
                new SpellCheckerCompounder(wordBreakSpellChecker, "field1", false),
                indexReader, false, false, new TrieMap<>(), 5, false,
                CharArraySet.EMPTY_SET);
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

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                new SpellCheckerWordBreaker(wordBreakSpellChecker, "field1", false),
                new SpellCheckerCompounder(wordBreakSpellChecker, "field1", false),
                indexReader, false, false, new TrieMap<>(), 5, false,
                CharArraySet.EMPTY_SET);
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

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                new SpellCheckerWordBreaker(wordBreakSpellChecker, "field1", false),
                new SpellCheckerCompounder(wordBreakSpellChecker, "field1", false),
                indexReader, false, true, new TrieMap<>(), 5, false,
                CharArraySet.EMPTY_SET);
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

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                new SpellCheckerWordBreaker(wordBreakSpellChecker, "field1", false),
                new SpellCheckerCompounder(wordBreakSpellChecker, "field1", false),
                indexReader, false, false, triggerWords, 5, false,
                CharArraySet.EMPTY_SET);
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
    public void testCompoundTriggerWordWithLowerCaseInputSetToFalse() throws IOException {
        TrieMap<Boolean> triggerWords = new TrieMap<>();
        triggerWords.put("Trigger_Upper", true);
        triggerWords.put("trigger_lower", true);

        // don't de-compound
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] {new SuggestWord[] {}});

        Map<List<String>, CombineSuggestion[]> suggestions = new HashMap<>();
        suggestions.put(Arrays.asList("w3", "w1"), new  CombineSuggestion[] { combineSuggestion("w3w1", 0, 1) });
        setupWordBreakMockWithCombinations(suggestions);

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                new SpellCheckerWordBreaker(wordBreakSpellChecker, "field1", false),
                new SpellCheckerCompounder(wordBreakSpellChecker, "field1", false),
                indexReader, false, false, triggerWords, 5, false,
                CharArraySet.EMPTY_SET);
        Query query = new Query();
        addTerm(query, "w1", false);
        addTerm(query, "Trigger_Upper", false);
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

        Query query2 = new Query();
        addTerm(query2, "w1", false);
        addTerm(query2, "trigger_upper", false);
        addTerm(query2, "w3", false);

        ExpandedQuery expandedQuery2 = new ExpandedQuery(query2);

        final ExpandedQuery rewritten2 = rewriter.rewrite(expandedQuery2);

        assertThat((Query) rewritten2.getUserQuery(),
                bq(
                        dmq(
                                term("w1", false)
                        ),
                        dmq(
                                term("trigger_upper", false)
                        ),
                        dmq(
                                term("w3", false)
                        )

                )
        );

        Query query3 = new Query();
        addTerm(query3, "w1", false);
        addTerm(query3, "Trigger_Lower", false);
        addTerm(query3, "w3", false);

        ExpandedQuery expandedQuery3 = new ExpandedQuery(query3);

        final ExpandedQuery rewritten3 = rewriter.rewrite(expandedQuery3);

        assertThat((Query) rewritten3.getUserQuery(),
                bq(
                        dmq(
                                term("w1", false)
                        ),
                        dmq(
                                term("Trigger_Lower", false)
                        ),
                        dmq(
                                term("w3", false)
                        )

                )
        );
    }

    @Test
    public void testCompoundTriggerWordWithLowerCaseInputSetToTrue() throws IOException {
        TrieMap<Boolean> triggerWords = new TrieMap<>();
        triggerWords.put("trigger_lower", true);

        // don't de-compound
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] {new SuggestWord[] {}});

        Map<List<String>, CombineSuggestion[]> suggestions = new HashMap<>();
        suggestions.put(Arrays.asList("w3", "w1"), new  CombineSuggestion[] { combineSuggestion("w3w1", 0, 1) });
        setupWordBreakMockWithCombinations(suggestions);

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                new SpellCheckerWordBreaker(wordBreakSpellChecker, "field1", false),
                new SpellCheckerCompounder(wordBreakSpellChecker, "field1", false),
                indexReader, true, false, triggerWords, 5, false,
                CharArraySet.EMPTY_SET);

        Query query1 = new Query();
        addTerm(query1, "w1", false);
        addTerm(query1, "trigger_lower", false);
        addTerm(query1, "w3", false);

        ExpandedQuery expandedQuery1 = new ExpandedQuery(query1);

        final ExpandedQuery rewritten1 = rewriter.rewrite(expandedQuery1);

        assertThat((Query) rewritten1.getUserQuery(),
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

        Query query2 = new Query();
        addTerm(query2, "w1", false);
        addTerm(query2, "Trigger_Lower", false);
        addTerm(query2, "w3", false);

        ExpandedQuery expandedQuery2 = new ExpandedQuery(query2);

        final ExpandedQuery rewritten2 = rewriter.rewrite(expandedQuery2);

        assertThat((Query) rewritten2.getUserQuery(),
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

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                new SpellCheckerWordBreaker(wordBreakSpellChecker, "field1", false),
                new SpellCheckerCompounder(wordBreakSpellChecker, "field1", false),
                indexReader,false, false, triggerWords, 5, false,
                CharArraySet.EMPTY_SET);
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

    @Test
    public void testThatDecompoundRespectsLowerCaseInputFalse() throws IOException {
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] { });

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                new SpellCheckerWordBreaker(wordBreakSpellChecker, "field1", false),
                new SpellCheckerCompounder(wordBreakSpellChecker, "field1", false),
                indexReader, false, false, new TrieMap<>(), 5, false,
                CharArraySet.EMPTY_SET);
        Query query = new Query();
        addTerm(query, "W1w2", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        rewriter.rewrite(expandedQuery);

        verify(wordBreakSpellChecker).suggestWordBreaks(eq(new Term("field1", "W1w2")), anyInt(), any(), any(), any());

    }

    @Test
    public void testThatDecompoundRespectsLowerCaseInputTrue() throws IOException {
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] { });

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                new SpellCheckerWordBreaker(wordBreakSpellChecker, "field1", true),
                new SpellCheckerCompounder(wordBreakSpellChecker, "field1", true),
                indexReader, true, false, new TrieMap<>(), 5, false,
                CharArraySet.EMPTY_SET);
        Query query = new Query();
        addTerm(query, "W1w2", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        rewriter.rewrite(expandedQuery);

        verify(wordBreakSpellChecker).suggestWordBreaks(eq(new Term("field1", "w1w2")), anyInt(), any(), any(), any());

    }

    @Test
    public void testThatCompoundRespectsLowerCaseInputTrue() throws IOException {
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] { });

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                new SpellCheckerWordBreaker(wordBreakSpellChecker, "field1", true),
                new SpellCheckerCompounder(wordBreakSpellChecker, "field1", true),
                indexReader, true, false, new TrieMap<>(), 5, false,
                CharArraySet.EMPTY_SET);
        Query query = new Query();
        addTerm(query, "W1", false);
        addTerm(query, "W2", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        rewriter.rewrite(expandedQuery);

        verify(wordBreakSpellChecker).suggestWordCombinations(eq(new Term[] {
                new Term("field1", "w1"), new Term("field1", "w2")}), anyInt(), any(), any());

    }

    @Test
    public void testThatCompoundRespectsLowerCaseInputFalse() throws IOException {
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] { });

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                new SpellCheckerWordBreaker(wordBreakSpellChecker, "field1", false),
                new SpellCheckerCompounder(wordBreakSpellChecker, "field1", false),
                indexReader, false, false, new TrieMap<>(), 5, false,
                CharArraySet.EMPTY_SET);
        Query query = new Query();
        addTerm(query, "W1", false);
        addTerm(query, "W2", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        rewriter.rewrite(expandedQuery);

        verify(wordBreakSpellChecker).suggestWordCombinations(eq(new Term[] {
                new Term("field1", "W1"), new Term("field1", "W2")}), anyInt(), any(), any());

    }

    @Test
    public void testCompoundingDoesNotCreateProtectedTerm() throws IOException {
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] { });

        Map<List<String>, CombineSuggestion[]> suggestions = new HashMap<>();
        suggestions.put(Arrays.asList("w1", "w2"), new CombineSuggestion[] { combineSuggestion("w1w2", 0, 1) });
        setupWordBreakMockWithCombinations(suggestions);

        CharArraySet protWord = new CharArraySet(Collections.singletonList("w1w2"), false);

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                new SpellCheckerWordBreaker(wordBreakSpellChecker, "field1", false),
                new SpellCheckerCompounder(wordBreakSpellChecker, "field1", false),
                indexReader, true, false, new TrieMap<>(), 5, false,
                protWord);

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
    public void testThatProtectedTermIsNotSplit() throws IOException {
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] { decompoundSuggestion("w1", "w2") });

        WordBreakCompoundRewriter rewriterWithNoProtectedWords = rewriter(CharArraySet.EMPTY_SET);
        CharArraySet protectedTerm = new CharArraySet(Collections.singletonList("w1w2"), false);
        WordBreakCompoundRewriter rewriterWhereW1W2IsProtected = rewriter(protectedTerm);

        final ExpandedQuery rewrittenNoProtected = rewriterWithNoProtectedWords.rewrite(new ExpandedQuery(query("w1w2")));

        assertThat((Query) rewrittenNoProtected.getUserQuery(),
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

        final ExpandedQuery rewrittenProtected = rewriterWhereW1W2IsProtected.rewrite(new ExpandedQuery(query("w1w2")));

        assertThat((Query) rewrittenProtected.getUserQuery(),
                bq(
                        dmq(
                                term("w1w2", false)
                        )
                )
        );
    }

    private WordBreakCompoundRewriter rewriter(CharArraySet protectedTerms) {
        return new WordBreakCompoundRewriter(
                new SpellCheckerWordBreaker(wordBreakSpellChecker, "field1", false),
                new SpellCheckerCompounder(wordBreakSpellChecker, "field1", false),
                indexReader, false, false, new TrieMap<>(), 5, false,
                protectedTerms
        );
    }

    private Query query(String term) {
        Query query = new Query();
        addTerm(query, term, false);
        return query;
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
