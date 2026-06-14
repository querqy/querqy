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
package querqy.rewrite.rules;

import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import querqy.model.Input;
import querqy.rewrite.commonrules.model.Instruction;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.InstructionsProperties;
import querqy.rewrite.commonrules.model.InstructionsSupplier;
import querqy.rewrite.commonrules.model.SynonymInstruction;
import querqy.rewrite.commonrules.model.Term;
import querqy.rewrite.rules.instruction.InstructionType;
import querqy.rewrite.rules.instruction.skeleton.InstructionSkeleton;
import querqy.rewrite.rules.rule.Rule;
import querqy.rewrite.rules.rule.skeleton.MultiLineParser;
import querqy.rewrite.rules.rule.skeleton.RuleSkeleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RuleParserTestUtils {

    public static List<Rule> rules(final Rule... rules) {
        return Arrays.asList(rules);
    }

    public static RuleBuilder ruleBuilder() {
        return RuleBuilder.create();
    }

    @Accessors(fluent = true)
    @Setter
    @NoArgsConstructor(staticName = "create")
    public static class RuleBuilder {
        private Input.SimpleInput input;
        private String id;
        private int ruleOrderNumber;

        private final List<Instruction> instructions = new ArrayList<>();
        private final Map<String, Object> properties = new HashMap<>();

        public RuleBuilder instruction(final Instruction instruction) {
            instructions.add(instruction);
            return this;
        }

        public RuleBuilder property(final String key, final Object value) {
            properties.put(key, value);
            return this;
        }

        public Rule build() {
            return Rule.of(
                    input,
                    new InstructionsSupplier(
                            new Instructions(
                                    ruleOrderNumber,
                                    id,
                                    instructions,
                                    new InstructionsProperties(properties)
                            )
                    )
            );
        }
    }

    public static RuleSkeleton.RuleSkeletonBuilder skeletonBuilder() {
        return RuleSkeleton.builder();
    }

    public static Input.SimpleInput input(final String input) {
        return new Input.SimpleInput(terms(input), input);
    }

    public static List<Term> terms(final String termString) {
        return Arrays.stream(termString.split(" "))
                .map(term -> new Term(term.toCharArray(), 0, term.length(), Collections.emptyList()))
                .collect(Collectors.toList());
    }

    public static Instruction synonym(final String termString) {
        return new SynonymInstruction(terms(termString));
    }

    public static List<RuleSkeleton> skeletons(final RuleSkeleton... ruleSkeletons) {
        return Arrays.asList(ruleSkeletons);
    }


    public static InstructionSkeleton synonymSkeleton(final String synonym) {
        return InstructionSkeleton.builder()
                .type(InstructionType.SYNONYM)
                .value(synonym)
                .build();
    }

    public static InstructionSkeleton upSkeleton(final String boostedTerm, final String parameter) {
        return InstructionSkeleton.builder()
                .type(InstructionType.UP)
                .parameter(parameter)
                .value(boostedTerm)
                .build();
    }

    public static String asText(final RuleSkeleton... ruleSkeletons) {
        return MultiLineParser.toTextDefinition(Arrays.asList(ruleSkeletons));
    }

}
