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
package querqy.rewriter.commonrules.rules.factory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import querqy.rewriter.commonrules.QuerqyParserFactory;
import querqy.rewriter.commonrules.model.TrieMapRulesCollectionBuilder;
import querqy.rewriter.commonrules.rules.RulesParser;
import querqy.rewriter.commonrules.rules.factory.config.RuleParserConfig;
import querqy.rewriter.commonrules.rules.factory.config.RulesParserConfig;
import querqy.rewriter.commonrules.rules.factory.config.TextParserConfig;

import java.io.Reader;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RulesParserFactory {

    public static RulesParser textParser(final RulesParserConfig rulesParserConfig) {
        return RulesParser.builder()
                .ruleSkeletonParser(
                        TextParserFactory.of(rulesParserConfig.getTextParserConfig()).createRuleSkeletonParser())
                .ruleParser(
                        RuleParserFactory.of(rulesParserConfig.getRuleParserConfig()).createRuleParser())
                .rulesCollectionBuilder(
                        rulesParserConfig.getRulesCollectionBuilder())
                .build();
    }

    /**
     * Convenience entry point for parsing (or just validating) a self-contained piece of common-rules text,
     * without having to assemble a {@link RulesParserConfig} by hand. Use the {@link #textParser(RulesParserConfig)}
     * overload instead if you need control over line-number mappings, allowed instruction types, boost method,
     * or a custom {@link querqy.rewriter.commonrules.model.RulesCollectionBuilder}.
     *
     * @param rulesContentReader The rules text to parse.
     * @param querqyParserFactory A parser for the right-hand side of rules.
     * @param allowBooleanInput Iff true, rule input can have boolean expressions.
     * @param ignoreCase Iff true, rule input matching is case-insensitive.
     */
    public static RulesParser textParser(final Reader rulesContentReader,
                                         final QuerqyParserFactory querqyParserFactory,
                                         final boolean allowBooleanInput,
                                         final boolean ignoreCase) {
        return textParser(RulesParserConfig.builder()
                .textParserConfig(TextParserConfig.builder()
                        .rulesContentReader(rulesContentReader)
                        .build())
                .ruleParserConfig(RuleParserConfig.builder()
                        .querqyParserFactory(querqyParserFactory)
                        .isAllowedToParseBooleanInput(allowBooleanInput)
                        .build())
                .rulesCollectionBuilder(new TrieMapRulesCollectionBuilder(ignoreCase))
                .build());
    }
}
