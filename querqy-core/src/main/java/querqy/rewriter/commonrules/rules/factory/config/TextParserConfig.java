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
package querqy.rewriter.commonrules.rules.factory.config;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;

import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

@Builder(builderClassName = "Builder")
@Getter
public class TextParserConfig {

    @Default private final Reader rulesContentReader = emtpyReader();
    @Default private final Map<Integer, Integer> lineNumberMappings = Collections.emptyMap();
    @Default private final boolean isMultiLineRulesConfig = true;

    public static TextParserConfig defaultConfig() {
        return TextParserConfig.builder().build();
    }

    private static Reader emtpyReader() {
        try (final StringReader stringReader = new StringReader("")) {
            return stringReader;
        }
    }

}
