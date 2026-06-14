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

import querqy.CompoundCharSequence;
import querqy.model.Term;
import querqy.rewrite.lookup.LookupConfig;
import querqy.rewrite.lookup.triemap.model.TrieMapSequence;
import querqy.trie.States;
import querqy.trie.TrieMap;


public class TrieMapSequenceLookup<ValueT> {

    private final TrieMap<ValueT> trieMap;
    private final LookupConfig lookupConfig;

    TrieMapSequenceLookup(final TrieMap<ValueT> trieMap, final LookupConfig lookupConfig) {
        this.trieMap = trieMap;
        this.lookupConfig = lookupConfig;
    }

    public States<ValueT> evaluateTerm(final Term term) {
        // TODO: why with field?
        final CharSequence lookupCharSequence = createLookupCharSequence(term);
        return trieMap.get(lookupCharSequence);
    }

    public States<ValueT> evaluateNextTerm(final TrieMapSequence<ValueT> sequence, final Term term) {
        final CharSequence lookupCharSequence = new CompoundCharSequence(
                null, " ", createLookupCharSequence(term));

        return trieMap.get(lookupCharSequence, sequence.getStates().getStateForCompleteSequence());
    }

    private CharSequence createLookupCharSequence(final Term term) {
        final CharSequence value = lookupConfig.getPreprocessor().process(term);
        final String field = term.getField();
        return (field == null) ? value : new CompoundCharSequence(":", field, value);
    }

}
