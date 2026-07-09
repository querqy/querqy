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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Accumulates {@link SuffixWildcardRule}s while rules are being parsed, then builds an immutable
 * {@link SuffixWildcardRules} lookup structure.
 *
 * @param <T> The value type associated with a rule (e.g. {@code InstructionsSupplier}).
 */
public class SuffixWildcardRulesBuilder<T> {

    private final TrieMap<List<SuffixWildcardRule<T>>> suffixTrieMap = new TrieMap<>();
    private final TrieMap<List<SuffixWildcardRule<T>>> leftContextTrieMap = new TrieMap<>();
    private final List<SuffixWildcardRule<T>> rulesWithoutLeftContext = new ArrayList<>();

    /**
     * Registers a leading-wildcard rule.
     *
     * @param suffixKey       The fixed (non-reversed) suffix text that the wildcard term requires, e.g. {@code hemd}
     *                        for a rule input of {@code *hemd}.
     * @param leftContextKey  The combined, space-joined key of the fixed terms preceding the wildcard term (and, if
     *                        the rule requires a left boundary, a leading boundary sentinel), or {@code null}/empty
     *                        if the wildcard term is not preceded by any required context.
     * @param rightTermKeys   The keys of the fixed terms following the wildcard term, in order (and, if the rule
     *                        requires a right boundary, a trailing boundary sentinel), or an empty list if none.
     * @param value           The rule's value (e.g. {@code InstructionsSupplier}).
     */
    public void addRule(final CharSequence suffixKey, final CharSequence leftContextKey,
                        final List<CharSequence> rightTermKeys, final T value) {

        final SuffixWildcardRule<T> rule = new SuffixWildcardRule<>(rightTermKeys, value);

        mergePrefixEntry(suffixTrieMap, new ReverseComparableCharSequence(suffixKey), rule);

        if (leftContextKey == null || leftContextKey.length() == 0) {
            rulesWithoutLeftContext.add(rule);
        } else {
            mergeExactEntry(leftContextTrieMap, leftContextKey, rule);
        }
    }

    public SuffixWildcardRules<T> build() {
        return new SuffixWildcardRules<>(suffixTrieMap, leftContextTrieMap,
                Collections.unmodifiableList(new ArrayList<>(rulesWithoutLeftContext)));
    }

    private void mergeExactEntry(final TrieMap<List<SuffixWildcardRule<T>>> trieMap, final CharSequence key,
                                 final SuffixWildcardRule<T> rule) {

        final State<List<SuffixWildcardRule<T>>> state = trieMap.get(key).getStateForCompleteSequence();
        if (state.isFinal()) {
            state.getValue().add(rule);
        } else {
            final List<SuffixWildcardRule<T>> rules = new ArrayList<>();
            rules.add(rule);
            trieMap.put(key, rules);
        }
    }

    private void mergePrefixEntry(final TrieMap<List<SuffixWildcardRule<T>>> trieMap, final CharSequence key,
                                  final SuffixWildcardRule<T> rule) {

        final States<List<SuffixWildcardRule<T>>> states = trieMap.get(key);

        final List<State<List<SuffixWildcardRule<T>>>> prefixes = states.getPrefixes();
        if (prefixes != null) {
            for (final State<List<SuffixWildcardRule<T>>> state : prefixes) {
                if (state.isFinal() && state.getIndex() == (key.length() - 1)) {
                    state.getValue().add(rule);
                    return;
                }
            }
        }

        final List<SuffixWildcardRule<T>> rules = new ArrayList<>();
        rules.add(rule);
        trieMap.putPrefix(key, rules);
    }

}
