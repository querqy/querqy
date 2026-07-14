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

import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewriter.numberunit.model.NumberUnitDefinition;
import querqy.rewriter.numberunit.model.PerUnitNumberUnitDefinition;
import querqy.trie.State;
import querqy.trie.TrieMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

public class NumberUnitRewriterFactory extends RewriterFactory {

    private final TrieMap<List<PerUnitNumberUnitDefinition>> numberUnitMap;
    private final NumberUnitQueryCreator numberUnitQueryCreator;

    /**
     * @param id                  The id of the rewriter
     * @param config              The rewriter's JSON configuration
     * @param queryCreatorFactory Builds the search-engine-specific {@link NumberUnitQueryCreator} from the
     *                            {@code scaleForLinearFunctions} value parsed out of {@code config}
     * @throws IOException if {@code config} is not valid JSON
     * @throws IllegalArgumentException if {@code config} does not satisfy the rewriter's validation rules
     */
    public NumberUnitRewriterFactory(final String id,
                                     final String config,
                                     final IntFunction<NumberUnitQueryCreator> queryCreatorFactory) throws IOException {
        super(id);
        final ParsedNumberUnitConfig parsedConfig = NumberUnitRewriterConfigParser.parse(config);
        this.numberUnitMap = createNumberUnitMap(parsedConfig.numberUnitDefinitions);
        this.numberUnitQueryCreator = queryCreatorFactory.apply(parsedConfig.scaleForLinearFunctions);
    }

    /**
     * @return a list of human-readable error messages; empty if {@code config} is valid
     */
    public static List<String> validateConfiguration(final String config) {
        return NumberUnitRewriterConfigParser.validate(config);
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
    public QueryRewriter createRewriter(final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return new NumberUnitRewriter(numberUnitMap, numberUnitQueryCreator);
    }

    @Override
    public Set<Term> getCacheableGenerableTerms() {
        return QueryRewriter.EMPTY_GENERABLE_TERMS;
    }
}
