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
package querqy.rewriter.commonrules.rules.rule.skeleton;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import querqy.rewrite.RuleParseException;
import querqy.rewriter.commonrules.rules.instruction.skeleton.InstructionSkeleton;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode
@ToString
public class RuleSkeleton {

    private final String inputSkeleton;
    private final List<InstructionSkeleton> instructionSkeletons;
    private final Map<String, Object> properties;

    public boolean hasProperties() {
        return !properties.isEmpty();
    }

    @Builder
    public static RuleSkeleton create(final String inputSkeleton,
                                      @Singular final List<InstructionSkeleton> instructionSkeletons,
                                      @Singular final Map<String, Object> properties) {

        if (inputSkeleton == null) {
            throw new RuleParseException("Rule has no input");
        }

        if (instructionSkeletons.isEmpty()) {
            throw new RuleParseException(String.format("Rule with input %s has no instructions", inputSkeleton));
        }

        return RuleSkeleton.of(inputSkeleton, instructionSkeletons, properties);
    }

    // make javadoc happy when working with Lombok
    public static class RuleSkeletonBuilder {}
}
