/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Querqy Contributors
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
package querqy.trie;

import querqy.ReverseComparableCharSequence;
import querqy.trie.model.SuffixMatch;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static querqy.trie.LookupUtils.COMPARE_STATE_BY_INDEX_DESC;

public class SuffixTrieMap<T> {

    private final TrieMap<T> trieMap;

    public SuffixTrieMap() {
        this.trieMap = new TrieMap<>();
    }

    public void putSuffix(final CharSequence seq, final T value) {
        this.putSuffix(seq, value, false);
    }

    public void putSuffix(final CharSequence seq, final T value, final boolean includeExactMatch) {
        if (seq.length() == 0) {
            throw new IllegalArgumentException("Must not put empty sequence into trie");
        }

        final ReverseComparableCharSequence revSeq = new ReverseComparableCharSequence(seq);
        trieMap.putPrefix(revSeq, value);

        if (includeExactMatch) {
            trieMap.put(revSeq, value);
        }
    }

    public Optional<SuffixMatch<T>> getBySuffix(final CharSequence seq) {
        if (seq.length() == 0) {
            return Optional.empty();
        }

        final ReverseComparableCharSequence revSeq = new ReverseComparableCharSequence(seq);

        final States<T> states = trieMap.get(revSeq);

        final State<T> fullMatch = states.getStateForCompleteSequence();
        if (fullMatch.isFinal()) {
            return Optional.of(new SuffixMatch<>(0, fullMatch.value));
        }

        final List<State<T>> suffixMatches = states.getPrefixes();
        if (suffixMatches != null && !suffixMatches.isEmpty()) {
            final State<T> suffixMaxMatch = Collections.max(states.getPrefixes(), COMPARE_STATE_BY_INDEX_DESC);

            final int startSubstring = seq.length() - (suffixMaxMatch.index + 1);

            return Optional.of(new SuffixMatch<>(
                    startSubstring,
                    seq.subSequence(0, startSubstring),
                    suffixMaxMatch.value));
        }

        return Optional.empty();
    }
}