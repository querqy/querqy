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
package querqy.rewrite.lookup.triemap.suffix;

import querqy.CompoundCharSequence;
import querqy.ReverseComparableCharSequence;
import querqy.trie.State;
import querqy.trie.States;
import querqy.trie.TrieMap;

import java.util.Collections;
import java.util.List;

/**
 * An immutable, isolated lookup structure for Common Rules whose input contains a leading-wildcard term, e.g.
 * {@code abc *hemd def}. Built by {@link SuffixWildcardRulesBuilder}.
 *
 * <p>Deliberately kept separate from the main, shared {@code TrieMap<InstructionsSupplier>} that
 * {@code TrieMapRulesCollectionBuilder} populates for all other (non-leading-wildcard) rules: a leading wildcard
 * cannot be represented in a single forward character trie (the wildcarded prefix characters of a query term
 * aren't known in advance to walk forward from), so this class reuses the same proven forward-trie mechanism for
 * the (purely literal) fixed terms before the wildcard, and a reverse trie (mirroring
 * {@code querqy.trie.SuffixTrieMap}) for the wildcard term itself.</p>
 */
public class SuffixWildcardRules<T> {

    private final TrieMap<List<SuffixWildcardRule<T>>> suffixTrieMap;
    private final TrieMap<List<SuffixWildcardRule<T>>> leftContextTrieMap;
    private final List<SuffixWildcardRule<T>> rulesWithoutLeftContext;

    SuffixWildcardRules(final TrieMap<List<SuffixWildcardRule<T>>> suffixTrieMap,
                       final TrieMap<List<SuffixWildcardRule<T>>> leftContextTrieMap,
                       final List<SuffixWildcardRule<T>> rulesWithoutLeftContext) {
        this.suffixTrieMap = suffixTrieMap;
        this.leftContextTrieMap = leftContextTrieMap;
        this.rulesWithoutLeftContext = rulesWithoutLeftContext == null
                ? Collections.emptyList() : rulesWithoutLeftContext;
    }

    private static final SuffixWildcardRules<?> EMPTY =
            new SuffixWildcardRules<>(new TrieMap<>(), new TrieMap<>(), Collections.emptyList());

    @SuppressWarnings("unchecked")
    public static <T> SuffixWildcardRules<T> empty() {
        return (SuffixWildcardRules<T>) EMPTY;
    }

    public boolean isEmpty() {
        return rulesWithoutLeftContext.isEmpty() && !suffixTrieMap.iterator().hasNext();
    }

    public List<SuffixWildcardRule<T>> getRulesWithoutLeftContext() {
        return rulesWithoutLeftContext;
    }

    /**
     * Looks up all leading-wildcard rules whose fixed suffix matches the end of {@code termKey}, capturing at
     * least one wildcarded character. Mirrors {@code querqy.trie.SuffixTrieMap#getBySuffix}, but collects *all*
     * matches (like {@code states.getPrefixes()}), not just the longest one, since Common Rules semantics allow
     * more than one rule to fire for the same term.
     */
    public States<List<SuffixWildcardRule<T>>> getSuffixStates(final CharSequence termKey) {
        return suffixTrieMap.get(new ReverseComparableCharSequence(termKey));
    }

    /**
     * Starts a fresh left-context match at {@code termKey} (mirroring {@code TrieMapSequenceLookup#evaluateTerm}).
     */
    public States<List<SuffixWildcardRule<T>>> getLeftContextStates(final CharSequence termKey) {
        return leftContextTrieMap.get(termKey);
    }

    /**
     * Extends a pending left-context match with one more term (mirroring
     * {@code TrieMapSequenceLookup#evaluateNextTerm}).
     */
    public States<List<SuffixWildcardRule<T>>> getNextLeftContextStates(final State<List<SuffixWildcardRule<T>>> priorState,
                                                                       final CharSequence termKey) {
        final CharSequence lookupCharSequence = new CompoundCharSequence(null, " ", termKey);
        return leftContextTrieMap.get(lookupCharSequence, priorState);
    }

}
