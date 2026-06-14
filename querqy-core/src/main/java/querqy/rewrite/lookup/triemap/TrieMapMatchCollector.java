/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2023 Querqy Contributors
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

import querqy.rewrite.commonrules.model.TermMatch;
import querqy.rewrite.commonrules.model.TermMatches;
import querqy.rewrite.lookup.model.Match;
import querqy.rewrite.lookup.triemap.model.TrieMapEvaluation;
import querqy.trie.State;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TrieMapMatchCollector<ValueT> {

    private final List<Match<ValueT>> matches = new ArrayList<>();

    TrieMapMatchCollector() {}

    public void collect(final TrieMapEvaluation<ValueT> trieMapEvaluation) {
        MatchExtractor.of(trieMapEvaluation).extractMatches()
                .forEach(matches::add);
    }

    public List<Match<ValueT>> getMatches() {
        return matches;
    }

    private static class MatchExtractor<T> {

        private final TrieMapEvaluation<T> trieMapEvaluation;

        private final Stream.Builder<Match<T>> matches = Stream.builder();

        private MatchExtractor(final TrieMapEvaluation<T> trieMapEvaluation) {
            this.trieMapEvaluation = trieMapEvaluation;
        }

        public Stream<Match<T>> extractMatches() {
            addCompleteMatch();
            addPrefixMatches();
            return matches.build();
        }

        private void addCompleteMatch() {
            final State<T> stateExactMatch = trieMapEvaluation.getStates().getStateForCompleteSequence();
            if (stateExactMatch.isFinal()) {
                matches.add(Match.of(createTermMatches(-1), stateExactMatch.getValue()));
            }
        }

        private TermMatches createTermMatches(final int matchIndex) {
            return Stream
                    .concat(
                            trieMapEvaluation.getPreviousTerms().stream()
                                    .map(TermMatch::new),
                            Stream.of(createLastTermMatch(matchIndex))
                    )
                    .filter(TermMatch::isExpandable)
                    .collect(Collectors.toCollection(TermMatches::new));
        }

        private TermMatch createLastTermMatch(final int matchIndex) {
            return matchIndex < 0
                    ? new TermMatch(trieMapEvaluation.getLastTerm())
                    : new TermMatch(trieMapEvaluation.getLastTerm(), true, matchIndex);
        }

        private void addPrefixMatches() {
            for (final State<T> prefixState : getPrefixStates()) {
                final T value = prefixState.getValue();

                if (value != null) {
                    matches.add(Match.of(createTermMatches(prefixState.getIndex()), value));
                }
            }
        }

        private List<State<T>> getPrefixStates() {
            final List<State<T>> prefixMatches = trieMapEvaluation.getStates().getPrefixes();
            return prefixMatches == null ? List.of() : prefixMatches;
        }

        public static <T> MatchExtractor<T> of(final TrieMapEvaluation<T> trieMapEvaluation) {
            return new MatchExtractor<>(trieMapEvaluation);
        }
    }

}
