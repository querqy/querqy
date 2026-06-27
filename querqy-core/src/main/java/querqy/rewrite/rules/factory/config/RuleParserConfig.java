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
package querqy.rewrite.rules.factory.config;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import querqy.rewriter.commonrules.QuerqyParserFactory;
import querqy.rewriter.commonrules.WhiteSpaceQuerqyParserFactory;
import querqy.rewriter.commonrules.model.BoostInstruction.BoostMethod;
import querqy.rewrite.rules.instruction.InstructionType;

import java.util.Set;

@Builder
@Getter
public class RuleParserConfig {

    @Default private final Set<InstructionType> allowedInstructionTypes = InstructionType.getAll();
    @Default private final QuerqyParserFactory querqyParserFactory = new WhiteSpaceQuerqyParserFactory();
    @Default private final boolean isAllowedToParseBooleanInput = false;
    @Default private final BoostMethod boostMethod = BoostMethod.ADDITIVE;

    public static RuleParserConfig defaultConfig() {
        return RuleParserConfig.builder().build();
    }

}
