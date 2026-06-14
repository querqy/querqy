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

public class TrieMapSequence<T> {

    private final States<T> states;
    private final List<Term> terms;

    private TrieMapSequence(final States<T> states, final List<Term> terms) {
        this.states = states;
        this.terms = terms;
    }

    public States<T> getStates() {
        return states;
    }

    public List<Term> getTerms() {
        return terms;
    }

    public static <T> TrieMapSequence<T> of(final States<T> states, final List<Term> terms) {
        return new TrieMapSequence<>(states, terms);
    }
}
