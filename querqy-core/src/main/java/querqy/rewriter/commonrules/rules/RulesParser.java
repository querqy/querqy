/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2021 Querqy Contributors
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
package querqy.rewriter.commonrules.rules;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import querqy.rewriter.commonrules.model.InstructionsSupplier;
import querqy.rewriter.commonrules.model.RulesCollectionBuilder;
import querqy.rewriter.commonrules.rules.rule.RuleParser;
import querqy.rewriter.commonrules.rules.rule.skeleton.RuleSkeleton;
import querqy.trie.TrieMap;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
public class RulesParser {

    private final RuleSkeletonParser ruleSkeletonParser;
    private final RuleParser ruleParser;
    private final RulesCollectionBuilder rulesCollectionBuilder;

    private int ruleOrderNumber = 0;

    @Builder
    private static RulesParser create(final RuleSkeletonParser ruleSkeletonParser,
                                      final RuleParser ruleParser,
                                      final RulesCollectionBuilder rulesCollectionBuilder) {
        return RulesParser.of(ruleSkeletonParser, ruleParser, rulesCollectionBuilder);
    }

    public TrieMap<InstructionsSupplier> parse() throws IOException {
        final List<RuleSkeleton> skeletons = ruleSkeletonParser.parse();
        parseRules(skeletons);

        return createTrieMap();
    }

    private void parseRules(final List<RuleSkeleton> skeletons) {
        for (final RuleSkeleton skeleton : skeletons) {
            ruleParser.parse(skeleton, ruleOrderNumber++);
        }
    }

    private TrieMap<InstructionsSupplier> createTrieMap() {
        ruleParser.finish().forEach(rulesCollectionBuilder::addRule);
        return rulesCollectionBuilder.getTrieMap();
    }
}
