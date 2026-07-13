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
package querqy.rewriter.commonrules.rules.rule;

import org.junit.Test;
import querqy.rewriter.commonrules.rules.RuleParseException;
import querqy.rewriter.commonrules.rules.factory.config.RuleParserConfig;
import querqy.rewriter.commonrules.rules.factory.RuleParserFactory;
import querqy.rewriter.commonrules.rules.property.PropertyParser;
import querqy.rewriter.commonrules.rules.rule.skeleton.RuleSkeleton;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static querqy.rewriter.commonrules.rules.RuleParserTestUtils.input;
import static querqy.rewriter.commonrules.rules.RuleParserTestUtils.rules;
import static querqy.rewriter.commonrules.rules.RuleParserTestUtils.skeletons;
import static querqy.rewriter.commonrules.rules.RuleParserTestUtils.synonym;
import static querqy.rewriter.commonrules.rules.RuleParserTestUtils.synonymSkeleton;
import static querqy.rewriter.commonrules.rules.RuleParserTestUtils.ruleBuilder;
import static querqy.rewriter.commonrules.rules.RuleParserTestUtils.skeletonBuilder;

public class RuleParserTest {

    @Test
    public void testThat_exceptionIsThrown_forRepeatedId() {
        final List<RuleSkeleton> skeletons = skeletons(
                skeletonBuilder()
                        .inputSkeleton("input1")
                        .instructionSkeleton(synonymSkeleton("synonym1"))
                        .property(PropertyParser.ID, "id")
                        .build(),
                skeletonBuilder()
                        .inputSkeleton("input2")
                        .instructionSkeleton(synonymSkeleton("synonym2"))
                        .property(PropertyParser.ID, "id")
                        .build()
        );

        assertThrows(RuleParseException.class, () -> parse(skeletons));
    }

    @Test
    public void testThat_rulesAreParsedProperly_forMultipleSkeletons() {
        final List<RuleSkeleton> skeletons = skeletons(
                skeletonBuilder()
                        .inputSkeleton("input1")
                        .instructionSkeleton(synonymSkeleton("synonym1"))
                        .property("key", "val")
                        .build(),
                skeletonBuilder()
                        .inputSkeleton("input2")
                        .instructionSkeleton(synonymSkeleton("synonym2"))
                        .property("key", "val")
                        .build()
        );

        final List<Rule> expectedRules = rules(
                ruleBuilder()
                        .input(input("input1"))
                        .ruleOrderNumber(0)
                        .id("input1#0")
                        .instruction(synonym("synonym1"))
                        .property("key", "val")
                        .property(PropertyParser.ID, "input1#0")
                        .property(PropertyParser.LOG_MESSAGE, "input1#0")
                        .build(),
                ruleBuilder()
                        .input(input("input2"))
                        .ruleOrderNumber(1)
                        .id("input2#1")
                        .instruction(synonym("synonym2"))
                        .property("key", "val")
                        .property(PropertyParser.ID, "input2#1")
                        .property(PropertyParser.LOG_MESSAGE, "input2#1")
                        .build()
        );

        final List<Rule> actualRules = parse(skeletons);

        assertThat(expectedRules).isEqualTo(actualRules);
    }

    private List<Rule> parse(final List<RuleSkeleton> ruleSkeletons) {
        final RuleParser parser = parser();

        IntStream.range(0, ruleSkeletons.size())
                .forEach(index -> parser.parse(ruleSkeletons.get(index), index));

        return parser.finish();
    }

    private static RuleParser parser() {
        return RuleParserFactory.of(RuleParserConfig.defaultConfig()).createRuleParser();
    }
}
