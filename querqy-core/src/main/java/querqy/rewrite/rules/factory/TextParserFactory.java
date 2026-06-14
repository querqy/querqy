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
import querqy.rewrite.rules.factory.config.TextParserConfig;
import querqy.rewrite.rules.input.skeleton.InputSkeletonParser;
import querqy.rewrite.rules.instruction.skeleton.InstructionSkeletonParser;
import querqy.rewrite.rules.property.skeleton.PropertySkeletonParser;
import querqy.rewrite.rules.rule.skeleton.LineParser;
import querqy.rewrite.rules.rule.skeleton.MultiLineParser;
import querqy.rewrite.rules.rule.skeleton.TextRuleSkeletonParser;
import querqy.rewrite.rules.rule.skeleton.SingleLineParser;

@RequiredArgsConstructor(staticName = "of")
public class TextParserFactory {

    private final TextParserConfig textParserConfig;

    public TextRuleSkeletonParser createRuleSkeletonParser() {
        final LineParser lineParser = createLineParser();

        return TextRuleSkeletonParser.builder()
                .rulesContentReader(textParserConfig.getRulesContentReader())
                .lineParser(lineParser)
                .lineNumberMappings(textParserConfig.getLineNumberMappings())
                .build();
    }

    private LineParser createLineParser() {
        return textParserConfig.isMultiLineRulesConfig()
                ? createMultiLineParser()
                : SingleLineParser.create();
    }

    private MultiLineParser createMultiLineParser() {
        return MultiLineParser.builder()
                .inputSkeletonParser(InputSkeletonParser.create())
                .instructionSkeletonParser(InstructionSkeletonParser.create())
                .propertySkeletonParser(PropertySkeletonParser.create())
                .build();
    }


}
