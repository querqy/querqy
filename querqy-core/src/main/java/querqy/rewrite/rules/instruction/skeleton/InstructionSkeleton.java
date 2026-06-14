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
package querqy.rewrite.rules.instruction.skeleton;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import querqy.rewrite.rules.instruction.InstructionType;

import java.util.Optional;

@Builder
@EqualsAndHashCode
@ToString
public class InstructionSkeleton {

    @Getter private final InstructionType type;
    private final String parameter;
    private final String value;

    public Optional<String> getParameter() {
        return Optional.ofNullable(parameter);
    }
    public Optional<String> getValue() {
        return Optional.ofNullable(value);
    }

}
