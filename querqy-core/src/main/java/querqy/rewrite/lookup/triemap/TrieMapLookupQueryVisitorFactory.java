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

import querqy.model.BooleanQuery;
import querqy.rewrite.lookup.LookupConfig;
import querqy.trie.TrieMap;


public class TrieMapLookupQueryVisitorFactory<ValueT> {

    private final TrieMap<ValueT> trieMap;
    private final LookupConfig lookupConfig;

    private TrieMapLookupQueryVisitorFactory(
            final TrieMap<ValueT> trieMap,
            final LookupConfig lookupConfig
    ) {
        this.trieMap = trieMap;
        this.lookupConfig = lookupConfig;
    }

    public TrieMapLookupQueryVisitor<ValueT> createTrieMapLookup(final BooleanQuery booleanQuery) {
        return new TrieMapLookupQueryVisitor<>(
                booleanQuery,
                lookupConfig,
                createAutomatonWrapper(),
                new TrieMapMatchCollector<>()
        );
    }

    private TrieMapSequenceLookup<ValueT> createAutomatonWrapper() {
        return new TrieMapSequenceLookup<>(trieMap, lookupConfig);
    }

    public TrieMap<ValueT> getTrieMap() {
        return trieMap;
    }

    public static <ValueT> TrieMapLookupQueryVisitorFactory<ValueT> of(final TrieMap<ValueT> trieMap, final LookupConfig lookupConfig) {
        return new TrieMapLookupQueryVisitorFactory<>(trieMap, lookupConfig);
    }

    public static <ValueT> TrieMapLookupQueryVisitorFactory<ValueT> of(final TrieMap<ValueT> trieMap) {
        return of(trieMap, LookupConfig.defaultConfig());
    }
}
