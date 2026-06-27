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
package querqy.rewriter.numberunit;

import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewriter.numberunit.NumberUnitQueryCreator;
import querqy.rewriter.numberunit.model.NumberUnitDefinition;
import querqy.rewriter.numberunit.model.PerUnitNumberUnitDefinition;
import querqy.trie.State;
import querqy.trie.TrieMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NumberUnitRewriterFactory extends RewriterFactory {

    private final TrieMap<List<PerUnitNumberUnitDefinition>> numberUnitMap;
    private final NumberUnitQueryCreator numberUnitQueryCreator;

    public NumberUnitRewriterFactory(final String id,
                                     final List<NumberUnitDefinition> numberUnitDefinitions,
                                     final NumberUnitQueryCreator numberUnitQueryCreator) {
        super(id);
        this.numberUnitMap = createNumberUnitMap(numberUnitDefinitions);
        this.numberUnitQueryCreator = numberUnitQueryCreator;
    }

    private TrieMap<List<PerUnitNumberUnitDefinition>> createNumberUnitMap(
            List<NumberUnitDefinition> numberUnitDefinitions) {

        final TrieMap<List<PerUnitNumberUnitDefinition>> map = new TrieMap<>();

        numberUnitDefinitions.forEach(numberUnitDefinition ->
                numberUnitDefinition.unitDefinitions.forEach(unitDefinition -> {

                    final State<List<PerUnitNumberUnitDefinition>> state = map.get(unitDefinition.term)
                            .getStateForCompleteSequence();

                    final PerUnitNumberUnitDefinition def = new PerUnitNumberUnitDefinition(numberUnitDefinition,
                            unitDefinition.multiplier);

                    if (state.isFinal()) {
                        state.value.add(def);

                    } else {
                        final List<PerUnitNumberUnitDefinition> newList = new ArrayList<>();
                        newList.add(def);
                        map.put(unitDefinition.term, newList);
                    }
                }));

        return map;
    }

    @Override
    public QueryRewriter createRewriter(final ExpandedQuery input,
                                        final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return new NumberUnitRewriter(numberUnitMap, numberUnitQueryCreator);
    }

    @Override
    public Set<Term> getCacheableGenerableTerms() {
        return QueryRewriter.EMPTY_GENERABLE_TERMS;
    }
}
