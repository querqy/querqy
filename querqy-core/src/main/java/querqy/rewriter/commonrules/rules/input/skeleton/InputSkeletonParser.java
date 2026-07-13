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
package querqy.rewriter.commonrules.rules.input.skeleton;

import lombok.NoArgsConstructor;
import querqy.rewriter.commonrules.rules.SkeletonComponentParser;

@NoArgsConstructor(staticName = "create")
public class InputSkeletonParser implements SkeletonComponentParser<String> {

    public static final String INPUT_INDICATOR = "=>";

    private String content = null;
    private String parsedInput = null;

    public void setContent(final String content) {
        this.content = content;
    }

    public boolean isParsable() {
        if (content == null) {
            throw new IllegalStateException("Content must be set before calling isParsable()");
        }

        return content.endsWith(INPUT_INDICATOR);
    }

    public void parse() {
        parsedInput = content.substring(0, content.length() - 2).trim();
    }

    public String finish() {
        if (content == null) {
            throw new IllegalStateException("Content must be parsed before finishing");
        }

        return parsedInput;
    }

    public static String toTextDefinition(final String inputDefinition) {
        return inputDefinition + " " + INPUT_INDICATOR;
    }
}