/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2026 Querqy Contributors
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
package querqy.rewrite.lookup.triemap;

import querqy.ComparableCharSequence;
import querqy.CompoundCharSequence;
import querqy.model.Term;
import querqy.rewrite.lookup.model.Match;
import querqy.rewrite.lookup.preprocessing.LookupPreprocessor;
import querqy.rewrite.lookup.triemap.suffix.SuffixWildcardRule;
import querqy.rewrite.lookup.triemap.suffix.SuffixWildcardRules;
import querqy.rewriter.commonrules.model.TermMatch;
import querqy.rewriter.commonrules.model.TermMatches;
import querqy.trie.State;
import querqy.trie.States;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Matches Common Rules whose input contains a leading-wildcard term (e.g. {@code abc *hemd def}) against a query,
 * as a companion to {@link TrieMapLookupQueryVisitor}. Kept as a separate class with a narrow, additive protocol
 * ({@link #visitTerm(Term)}, {@link #nextPosition()}, {@link #getMatches()}) so that the visitor's own, more
 * intricate state machine for the (unrelated) exact-match/trailing-wildcard trie does not need to be reworked.
 *
 * <p>A rule's fixed terms before the wildcard ("left context") are matched via a small, separate forward trie
 * (see {@link SuffixWildcardRules}), threaded across positions the same way {@link TrieMapLookupQueryVisitor}
 * threads its own trie states. The wildcard term itself is matched via a reverse trie. Any fixed terms after the
 * wildcard ("right context") are checked directly, term by term, against the query for the (rare) in-flight
 * candidate right after a suffix match.</p>
 */
public class SuffixWildcardMatcher<T> {

    private final SuffixWildcardRules<T> rules;
    private final LookupPreprocessor preprocessor;
    private final Set<SuffixWildcardRule<T>> rulesWithoutLeftContext;

    private final List<Match<T>> matches = new ArrayList<>();

    private Map<SuffixWildcardRule<T>, List<Term>> readyForWildcard = Collections.emptyMap();
    private Map<SuffixWildcardRule<T>, List<Term>> nextReadyForWildcard = new HashMap<>();

    private List<LeftContextPending<T>> previousLeftContextPending = Collections.emptyList();
    private List<LeftContextPending<T>> leftContextPending = new ArrayList<>();

    private List<RightContextPending<T>> previousRightContextPending = Collections.emptyList();
    private List<RightContextPending<T>> rightContextPending = new ArrayList<>();

    public SuffixWildcardMatcher(final SuffixWildcardRules<T> rules, final LookupPreprocessor preprocessor) {
        this.rules = rules;
        this.preprocessor = preprocessor;
        this.rulesWithoutLeftContext = new HashSet<>(rules.getRulesWithoutLeftContext());
    }

    public void visitTerm(final Term term) {
        if (rules.isEmpty()) {
            return;
        }

        final CharSequence termKey = createLookupCharSequence(term);

        advanceRightContext(term, termKey);
        checkSuffixMatch(term, termKey);
        advanceLeftContext(term, termKey);
    }

    /**
     * Rolls pending state forward to the next query position. Must be called exactly where
     * {@code TrieMapLookupQueryVisitor} refreshes its own sequence lists, so that left/right context spanning
     * multiple positions lines up with the alternatives (DisMax terms) visited within a single position.
     */
    public void nextPosition() {
        previousLeftContextPending = leftContextPending;
        leftContextPending = new ArrayList<>();

        readyForWildcard = nextReadyForWildcard;
        nextReadyForWildcard = new HashMap<>();

        previousRightContextPending = rightContextPending;
        rightContextPending = new ArrayList<>();
    }

    public List<Match<T>> getMatches() {
        return matches;
    }

    private void advanceRightContext(final Term term, final CharSequence termKey) {
        for (final RightContextPending<T> pending : previousRightContextPending) {
            final CharSequence expectedKey = pending.rule.getRightTermKeys().get(pending.nextRightTermIndex);
            if (!contentEquals(expectedKey, termKey)) {
                continue;
            }

            final TermMatches extended = new TermMatches();
            extended.addAll(pending.termMatchesSoFar);
            if (isExpandable(term)) {
                extended.add(new TermMatch(term));
            }

            final int nextIndex = pending.nextRightTermIndex + 1;
            if (nextIndex == pending.rule.getRightTermKeys().size()) {
                matches.add(Match.of(extended, pending.rule.getValue()));
            } else {
                rightContextPending.add(new RightContextPending<>(pending.rule, extended, nextIndex));
            }
        }
    }

    private void checkSuffixMatch(final Term term, final CharSequence termKey) {
        final States<List<SuffixWildcardRule<T>>> states = rules.getSuffixStates(termKey);
        final List<State<List<SuffixWildcardRule<T>>>> prefixes = states.getPrefixes();
        if (prefixes == null) {
            return;
        }

        for (final State<List<SuffixWildcardRule<T>>> state : prefixes) {
            if (state.getValue() == null) {
                continue;
            }

            final ComparableCharSequence wildcardMatch = term.subSequence(0, term.length() - (state.getIndex() + 1));

            for (final SuffixWildcardRule<T> rule : state.getValue()) {

                final List<Term> leftContextTerms;
                if (rulesWithoutLeftContext.contains(rule)) {
                    leftContextTerms = Collections.emptyList();
                } else if (readyForWildcard.containsKey(rule)) {
                    leftContextTerms = readyForWildcard.get(rule);
                } else {
                    continue;
                }

                final TermMatches termMatches = new TermMatches();
                for (final Term leftTerm : leftContextTerms) {
                    if (isExpandable(leftTerm)) {
                        termMatches.add(new TermMatch(leftTerm));
                    }
                }
                termMatches.add(new TermMatch(term, true, wildcardMatch));

                if (rule.getRightTermKeys().isEmpty()) {
                    matches.add(Match.of(termMatches, rule.getValue()));
                } else {
                    rightContextPending.add(new RightContextPending<>(rule, termMatches, 0));
                }
            }
        }
    }

    private void advanceLeftContext(final Term term, final CharSequence termKey) {

        for (final LeftContextPending<T> pending : previousLeftContextPending) {
            final States<List<SuffixWildcardRule<T>>> states = rules.getNextLeftContextStates(pending.state, termKey);
            registerLeftContextStates(states, concat(pending.matchedTerms, term));
        }

        final States<List<SuffixWildcardRule<T>>> freshStates = rules.getLeftContextStates(termKey);
        registerLeftContextStates(freshStates, Collections.singletonList(term));
    }

    private void registerLeftContextStates(final States<List<SuffixWildcardRule<T>>> states,
                                           final List<Term> matchedTerms) {

        final State<List<SuffixWildcardRule<T>>> completeState = states.getStateForCompleteSequence();
        if (!completeState.isKnown()) {
            return;
        }

        leftContextPending.add(new LeftContextPending<>(completeState, matchedTerms));

        if (completeState.isFinal()) {
            for (final SuffixWildcardRule<T> rule : completeState.getValue()) {
                nextReadyForWildcard.put(rule, matchedTerms);
            }
        }
    }

    private boolean isExpandable(final Term term) {
        return term.getParent() != null;
    }

    private boolean contentEquals(final CharSequence a, final CharSequence b) {
        if (a.length() != b.length()) {
            return false;
        }
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private List<Term> concat(final List<Term> terms, final Term term) {
        final List<Term> result = new ArrayList<>(terms.size() + 1);
        result.addAll(terms);
        result.add(term);
        return result;
    }

    private CharSequence createLookupCharSequence(final Term term) {
        final CharSequence value = preprocessor.process(term);
        final String field = term.getField();
        return (field == null) ? value : new CompoundCharSequence(":", field, value);
    }

    private static class LeftContextPending<T> {
        final State<List<SuffixWildcardRule<T>>> state;
        final List<Term> matchedTerms;

        LeftContextPending(final State<List<SuffixWildcardRule<T>>> state, final List<Term> matchedTerms) {
            this.state = state;
            this.matchedTerms = matchedTerms;
        }
    }

    private static class RightContextPending<T> {
        final SuffixWildcardRule<T> rule;
        final TermMatches termMatchesSoFar;
        final int nextRightTermIndex;

        RightContextPending(final SuffixWildcardRule<T> rule, final TermMatches termMatchesSoFar,
                            final int nextRightTermIndex) {
            this.rule = rule;
            this.termMatchesSoFar = termMatchesSoFar;
            this.nextRightTermIndex = nextRightTermIndex;
        }
    }
}
