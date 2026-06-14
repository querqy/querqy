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
package querqy.rewrite.lookup.triemap.model;

import querqy.model.Term;
import querqy.trie.States;

import java.util.List;

public class TrieMapEvaluation<ValueT> {

    private final List<Term> previousTerms;
    private final Term lastTerm;
    private final States<ValueT> states;

    private TrieMapEvaluation(final List<Term> previousTerms, final Term lastTerm, final States<ValueT> states) {
        this.previousTerms = previousTerms;
        this.lastTerm = lastTerm;
        this.states = states;
    }

    public List<Term> getPreviousTerms() {
        return previousTerms;
    }

    public Term getLastTerm() {
        return lastTerm;
    }

    public States<ValueT> getStates() {
        return states;
    }

    public static <ValueT> TrieMapEvaluation<ValueT> of(final List<Term> previousTerms, final Term lastTerm, final States<ValueT> states) {
        return new TrieMapEvaluation<>(previousTerms, lastTerm, states);
    }
}
