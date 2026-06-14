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
package querqy.rewrite.rules.factory;

import lombok.RequiredArgsConstructor;
import querqy.rewrite.rules.factory.config.RuleParserConfig;
import querqy.rewrite.rules.input.InputParserAdapter;
import querqy.rewrite.rules.instruction.InstructionParser;
import querqy.rewrite.rules.property.PropertyParser;
import querqy.rewrite.rules.query.QuerqyQueryParser;
import querqy.rewrite.rules.query.TermsParser;
import querqy.rewrite.rules.rule.RuleParser;

@RequiredArgsConstructor(staticName = "of")
public class RuleParserFactory {

    private final RuleParserConfig ruleParserConfig;

    public RuleParser createRuleParser() {
        return RuleParser.builder()
                .inputParser(createInputParserAdapter())
                .instructionParser(createInstructionParser())
                .propertyParser(PropertyParser.create())
                .build();
    }

    private InputParserAdapter createInputParserAdapter() {
        return InputParserAdapter.builder()
                .isAllowedToParseBooleanInput(ruleParserConfig.isAllowedToParseBooleanInput())
                .build();
    }

    private InstructionParser createInstructionParser() {
        return InstructionParser.prototypeBuilder()
                .querqyQueryParser(
                        QuerqyQueryParser.createPrototypeOf(ruleParserConfig.getQuerqyParserFactory()))
                .termsParser(TermsParser.createPrototype())
                .supportedTypes(ruleParserConfig.getAllowedInstructionTypes())
                .boostMethod(ruleParserConfig.getBoostMethod())
                .build();
    }

}
