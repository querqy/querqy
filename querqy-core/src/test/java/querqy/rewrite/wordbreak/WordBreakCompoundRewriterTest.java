/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018 Querqy Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package querqy.rewrite.contrib.wordbreak;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.model.StringRawQuery;
import querqy.rewrite.RewriterOutput;
import querqy.rewrite.contrib.wordbreak.Compounder;
import querqy.rewrite.contrib.wordbreak.TermCorpus;
import querqy.rewrite.contrib.wordbreak.WordBreaker;
import querqy.rewrite.contrib.wordbreak.WordBreakCompoundRewriter;
import querqy.trie.TrieMap;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static querqy.QuerqyMatchers.*;

@RunWith(MockitoJUnitRunner.class)
public class WordBreakCompoundRewriterTest {

    @Mock
    WordBreaker wordBreaker;

    @Mock
    Compounder compounder;

    @Mock
    TermCorpus termCorpus;

    private static final TrieMap<Boolean> NO_TRIGGERWORDS = new TrieMap<>();
    private static final TrieMap<Boolean> NO_PROTECTEDWORDS = new TrieMap<>();

    @Test
    public void testNoDecompoundForSingleToken() throws IOException {

        when(wordBreaker.breakWord(any(), any(), anyInt(), anyBoolean()))
                .thenReturn(Collections.emptyList());

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                wordBreaker, compounder, termCorpus, false, false, NO_TRIGGERWORDS, 5, false,
                NO_PROTECTEDWORDS);
        Query query = new Query();
        addTerm(query, "w1w2", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery, null).getExpandedQuery();

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
        when(wordBreaker.breakWord(any(), any(), anyInt(), anyBoolean()))
                .thenReturn(List.<CharSequence[]>of(new CharSequence[]{"w1", "w2"}));

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                wordBreaker, compounder, termCorpus, false, false, NO_TRIGGERWORDS, 5, false,
                NO_PROTECTEDWORDS);
        Query query = new Query();
        addTerm(query, "w1w2", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery, null).getExpandedQuery();

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
                wordBreaker, compounder, termCorpus, false, false, NO_TRIGGERWORDS, 5, false,
                NO_PROTECTEDWORDS);
        Query query = new Query();
        addTerm(query, "w1w2", true);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery, null).getExpandedQuery();

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
        when(wordBreaker.breakWord(any(), any(), anyInt(), anyBoolean()))
                .thenReturn(Collections.emptyList());

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                wordBreaker, compounder, termCorpus, false, false, NO_TRIGGERWORDS, 5, false,
                NO_PROTECTEDWORDS);
        Query query = new Query();
        addTerm(query, "w1", false);
        addTerm(query, "w2", true);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery, null).getExpandedQuery();

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
        when(wordBreaker.breakWord(any(), any(), anyInt(), anyBoolean()))
                .thenReturn(Collections.emptyList());

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                wordBreaker, compounder, termCorpus, false, false, NO_TRIGGERWORDS, 5, false,
                NO_PROTECTEDWORDS);
        Query query = new Query();
        addTerm(query, "w1", true);
        addTerm(query, "w2", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery, null).getExpandedQuery();

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
        when(wordBreaker.breakWord(any(), any(), anyInt(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(compounder.combine(any(), any(), anyBoolean()))
                .thenAnswer(invocation -> {
                    querqy.model.Term[] terms = invocation.getArgument(0);
                    return List.of(new Compounder.CompoundTerm("w1w2", terms));
                });

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                wordBreaker, compounder, termCorpus, false, false, NO_TRIGGERWORDS, 5, false,
                NO_PROTECTEDWORDS);
        Query query = new Query();
        addTerm(query, "w1", false);
        addTerm(query, "w2g", true);
        addTerm(query, "w2", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery, null).getExpandedQuery();

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
        when(wordBreaker.breakWord(any(), any(), anyInt(), anyBoolean()))
                .thenReturn(List.<CharSequence[]>of(new CharSequence[]{"w1", "w2"}, new CharSequence[]{"w", "1w2"}));

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                wordBreaker, compounder, termCorpus, false, false, NO_TRIGGERWORDS, 5, false,
                NO_PROTECTEDWORDS);
        Query query = new Query();
        addTerm(query, "w1w2", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery, null).getExpandedQuery();

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
        when(wordBreaker.breakWord(any(), any(), eq(2), anyBoolean()))
                .thenReturn(List.<CharSequence[]>of(new CharSequence[]{"w3", "w4"}, new CharSequence[]{"w", "3w4"}));

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                wordBreaker, compounder, termCorpus, false, false, NO_TRIGGERWORDS, 2, false,
                NO_PROTECTEDWORDS);
        Query query = new Query();
        addTerm(query, "w3w4", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery, null).getExpandedQuery();

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
        when(wordBreaker.breakWord(any(), any(), anyInt(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(compounder.combine(any(), any(), eq(false)))
                .thenAnswer(invocation -> {
                    querqy.model.Term[] terms = invocation.getArgument(0);
                    return List.of(new Compounder.CompoundTerm("w1w2", terms));
                });

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                wordBreaker, compounder, termCorpus, false, false, NO_TRIGGERWORDS, 5, false,
                NO_PROTECTEDWORDS);
        Query query = new Query();
        addTerm(query, "w1", false);
        addTerm(query, "w2", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery, null).getExpandedQuery();

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
        when(wordBreaker.breakWord(any(), any(), anyInt(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(compounder.combine(any(), any(), anyBoolean()))
                .thenReturn(Collections.emptyList());

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                wordBreaker, compounder, termCorpus, false, false, NO_TRIGGERWORDS, 5, false,
                NO_PROTECTEDWORDS);
        Query query = new Query();
        addTerm(query, "w1", false);
        addTerm(query, "w2", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery, null).getExpandedQuery();

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
        when(wordBreaker.breakWord(any(), any(), anyInt(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(compounder.combine(any(), any(), eq(false)))
                .thenAnswer(invocation -> {
                    querqy.model.Term[] terms = invocation.getArgument(0);
                    return List.of(new Compounder.CompoundTerm("w1w2", terms));
                });
        when(compounder.combine(any(), any(), eq(true)))
                .thenAnswer(invocation -> {
                    querqy.model.Term[] terms = invocation.getArgument(0);
                    return List.of(new Compounder.CompoundTerm("w2w1", terms));
                });

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                wordBreaker, compounder, termCorpus, false, true, NO_TRIGGERWORDS, 5, false,
                NO_PROTECTEDWORDS);
        Query query = new Query();
        addTerm(query, "w1", false);
        addTerm(query, "w2", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery, null).getExpandedQuery();

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

        when(wordBreaker.breakWord(any(), any(), anyInt(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(compounder.combine(any(), any(), eq(false)))
                .thenReturn(Collections.emptyList());
        when(compounder.combine(any(), any(), eq(true)))
                .thenAnswer(invocation -> {
                    querqy.model.Term[] terms = invocation.getArgument(0);
                    return List.of(new Compounder.CompoundTerm("w3w1", terms));
                });

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                wordBreaker, compounder, termCorpus, false, false, triggerWords, 5, false,
                NO_PROTECTEDWORDS);
        Query query = new Query();
        addTerm(query, "w1", false);
        addTerm(query, "trigger", false);
        addTerm(query, "w3", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery, null).getExpandedQuery();

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

        when(wordBreaker.breakWord(any(), any(), anyInt(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(compounder.combine(any(), any(), eq(false)))
                .thenReturn(Collections.emptyList());
        when(compounder.combine(any(), any(), eq(true)))
                .thenAnswer(invocation -> {
                    querqy.model.Term[] terms = invocation.getArgument(0);
                    return List.of(new Compounder.CompoundTerm("w3w1", terms));
                });

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                wordBreaker, compounder, termCorpus, false, false, triggerWords, 5, false,
                NO_PROTECTEDWORDS);
        Query query = new Query();
        addTerm(query, "w1", false);
        addTerm(query, "Trigger_Upper", false);
        addTerm(query, "w3", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery, null).getExpandedQuery();

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

        final ExpandedQuery rewritten2 = rewriter.rewrite(expandedQuery2, null).getExpandedQuery();

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

        final ExpandedQuery rewritten3 = rewriter.rewrite(expandedQuery3, null).getExpandedQuery();

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

        when(wordBreaker.breakWord(any(), any(), anyInt(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(compounder.combine(any(), any(), eq(false)))
                .thenReturn(Collections.emptyList());
        when(compounder.combine(any(), any(), eq(true)))
                .thenAnswer(invocation -> {
                    querqy.model.Term[] terms = invocation.getArgument(0);
                    return List.of(new Compounder.CompoundTerm("w3w1", terms));
                });

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                wordBreaker, compounder, termCorpus, true, false, triggerWords, 5, false,
                NO_PROTECTEDWORDS);

        Query query1 = new Query();
        addTerm(query1, "w1", false);
        addTerm(query1, "trigger_lower", false);
        addTerm(query1, "w3", false);

        ExpandedQuery expandedQuery1 = new ExpandedQuery(query1);

        final ExpandedQuery rewritten1 = rewriter.rewrite(expandedQuery1, null).getExpandedQuery();

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

        final ExpandedQuery rewritten2 = rewriter.rewrite(expandedQuery2, null).getExpandedQuery();

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

        when(wordBreaker.breakWord(any(), any(), anyInt(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(compounder.combine(any(), any(), anyBoolean()))
                .thenAnswer(invocation -> {
                    querqy.model.Term[] terms = invocation.getArgument(0);
                    boolean reverse = invocation.getArgument(2);
                    String left = terms[0].getValue().toString();
                    String right = terms[1].getValue().toString();
                    if ("w0".equals(left) && "w1".equals(right) && !reverse) {
                        return List.of(new Compounder.CompoundTerm("w0w1", terms));
                    } else if ("w1".equals(left) && "w3".equals(right) && reverse) {
                        return List.of(new Compounder.CompoundTerm("w3w1", terms));
                    } else if ("w3".equals(left) && "w4".equals(right) && !reverse) {
                        return List.of(new Compounder.CompoundTerm("w3w4", terms));
                    }
                    return Collections.emptyList();
                });

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                wordBreaker, compounder, termCorpus, false, false, triggerWords, 5, false,
                NO_PROTECTEDWORDS);
        Query query = new Query();
        addTerm(query, "w0", false);
        addTerm(query, "w1", false);
        addTerm(query, "trigger", false);
        addTerm(query, "w3", false);
        addTerm(query, "w4", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery, null).getExpandedQuery();

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
    public void testCompoundingDoesNotCreateProtectedTerm() throws IOException {
        when(wordBreaker.breakWord(any(), any(), anyInt(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(compounder.combine(any(), any(), eq(false)))
                .thenAnswer(invocation -> {
                    querqy.model.Term[] terms = invocation.getArgument(0);
                    return List.of(new Compounder.CompoundTerm("w1w2", terms));
                });

        TrieMap<Boolean> protWord = new TrieMap<>();
        protWord.put("w1w2", true);

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                wordBreaker, compounder, termCorpus, true, false, NO_TRIGGERWORDS, 5, false,
                protWord);

        Query query = new Query();
        addTerm(query, "w1", false);
        addTerm(query, "w2", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery, null).getExpandedQuery();

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
        when(wordBreaker.breakWord(any(), any(), anyInt(), anyBoolean()))
                .thenReturn(List.<CharSequence[]>of(new CharSequence[]{"w1", "w2"}));

        WordBreakCompoundRewriter rewriterWithNoProtectedWords = rewriter(NO_PROTECTEDWORDS);

        TrieMap<Boolean> protectedTerm = new TrieMap<>();
        protectedTerm.put("w1w2", true);
        WordBreakCompoundRewriter rewriterWhereW1W2IsProtected = rewriter(protectedTerm);

        final ExpandedQuery rewrittenNoProtected = rewriterWithNoProtectedWords.rewrite(new ExpandedQuery(query("w1w2")), null).getExpandedQuery();

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

        final ExpandedQuery rewrittenProtected = rewriterWhereW1W2IsProtected.rewrite(new ExpandedQuery(query("w1w2")), null).getExpandedQuery();

        assertThat((Query) rewrittenProtected.getUserQuery(),
                bq(
                        dmq(
                                term("w1w2", false)
                        )
                )
        );
    }

    @Test
    public void testThatRawQueryAsUserQueryIsJustPassedUnchanged() throws Exception {

        final StringRawQuery userQuery = new StringRawQuery(null, "{!terms f=id}123", Clause.Occur.MUST, false);
        final ExpandedQuery query = new ExpandedQuery(userQuery);

        final WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                wordBreaker, compounder, termCorpus, false, false, NO_TRIGGERWORDS, 5, false,
                NO_PROTECTEDWORDS);

        final RewriterOutput output = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter());
        assertEquals(userQuery, output.getExpandedQuery().getUserQuery());
    }

    private WordBreakCompoundRewriter rewriter(TrieMap<Boolean> protectedTerms) {
        return new WordBreakCompoundRewriter(wordBreaker, compounder, termCorpus,
                false, false, new TrieMap<>(), 5, false, protectedTerms);
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
}
