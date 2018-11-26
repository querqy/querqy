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
import org.apache.lucene.search.spell.SuggestMode;
import org.apache.lucene.search.spell.SuggestWord;
import org.apache.lucene.search.spell.WordBreakSpellChecker;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.trie.TrieMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

        SuggestWord word1 = new SuggestWord();
        word1.string = "w1";

        SuggestWord word2 = new SuggestWord();
        word2.string = "w2";

        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] {new SuggestWord[] {word1, word2}});

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
    public void testCompoundTwoInputTokensOnly() throws IOException {
        // don't de-compound
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] {new SuggestWord[] {}});

        SuggestWord combi = new SuggestWord();
        combi.string = "w1w2";

        CombineSuggestion combineSuggestion = new CombineSuggestion(combi, new int[] {0, 1});

        when(wordBreakSpellChecker.suggestWordCombinations(any(), anyInt(), any(), any()))
                .thenReturn(new  CombineSuggestion[] {combineSuggestion });

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
    public void testCompoundForTwoNonAdjacentInputTokens() throws IOException {
        // don't de-compound
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] {new SuggestWord[] {}});

        SuggestWord combi = new SuggestWord();
        combi.string = "w1w3";

        CombineSuggestion combineSuggestion = new CombineSuggestion(combi, new int[] {0, 2});

        when(wordBreakSpellChecker.suggestWordCombinations(any(), anyInt(), any(), any()))
                .thenReturn(new  CombineSuggestion[] { })
                .thenReturn(new  CombineSuggestion[] {combineSuggestion});

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(wordBreakSpellChecker, indexReader, "field1",
                false, new TrieMap<>());
        Query query = new Query();
        addTerm(query, "w1", false);
        addTerm(query, "w2", false);
        addTerm(query, "w3", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery);

        assertThat((Query) rewritten.getUserQuery(),
                bq(
                        dmq(
                                term("w1", false),
                                term("w1w3", true)
                        ),
                        dmq(
                                term("w2", false)
                        ),
                        dmq(
                                term("w3", false),
                                term("w1w3", true)
                        )

                )
        );
    }

    @Test
    public void testAlwaysAddRevertCompoundsForTwoWordInput() throws IOException {

        // don't de-compound
        when(wordBreakSpellChecker.suggestWordBreaks(any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] {new SuggestWord[] {}});

        SuggestWord combi1 = new SuggestWord();
        combi1.string = "w1w2";
        CombineSuggestion combineSuggestion1 = new CombineSuggestion(combi1, new int[] {0, 1});

        SuggestWord combi2 = new SuggestWord();
        combi2.string = "w2w1";
        CombineSuggestion combineSuggestion2 = new CombineSuggestion(combi2, new int[] {0, 1});

        when(wordBreakSpellChecker.suggestWordCombinations(any(), anyInt(), any(), any()))
                .thenReturn(new  CombineSuggestion[] {combineSuggestion1})
                .thenReturn(new  CombineSuggestion[] {combineSuggestion2});

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

        SuggestWord combi = new SuggestWord();
        combi.string = "w3w1";

        CombineSuggestion combineSuggestion = new CombineSuggestion(combi, new int[] {0, 1});

        when(wordBreakSpellChecker.suggestWordCombinations(any(), anyInt(), any(), any()))
                .thenReturn(new  CombineSuggestion[] { })
                .thenReturn(new  CombineSuggestion[] {combineSuggestion});

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
                                term("trigger", false)
                        ),
                        dmq(
                                term("w3", false),
                                term("w3w1", true)
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
}
