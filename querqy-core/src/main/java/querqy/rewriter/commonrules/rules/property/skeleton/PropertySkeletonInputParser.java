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
package querqy.rewriter.commonrules.rules.property.skeleton;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "of")
public class PropertySkeletonInputParser {

    public static final String PROPERTY_INDICATOR = "@";
    public static final String MULTILINE_INITIATION_INDICATOR = "{";
    public static final String MULTILINE_FINISHING_INDICATOR = "}@";
    public static final String ESCAPED_MULTILINE_FINISHING_INDICATOR = "\\}@";

    private final String propertyInput;

    private final PropertySkeletonInput.Builder builder = PropertySkeletonInput.builder();
    private String parsedPropertyInput;

    public PropertySkeletonInput parse() {
        builder.rawInput(propertyInput);
        parsedPropertyInput = propertyInput;

        parsePrefix();
        parseSuffix();

        return builder.strippedInput(parsedPropertyInput).build();
    }

    private void parsePrefix() {
        if (parsedPropertyInput.startsWith(PROPERTY_INDICATOR)) {
            builder.isPropertyInitiation(true);
            removeFirstCharFromInput();
            checkForMultiLineInitiation();
        }
    }

    private void removeFirstCharFromInput() {
        parsedPropertyInput = parsedPropertyInput.substring(1);
    }

    private void checkForMultiLineInitiation() {
        if (parsedPropertyInput.startsWith(MULTILINE_INITIATION_INDICATOR)) {
            builder.isMultiLineInitiation(true);
        }
    }

    private void parseSuffix() {
        if (parsedPropertyInput.endsWith(MULTILINE_FINISHING_INDICATOR) &&
                !parsedPropertyInput.endsWith(ESCAPED_MULTILINE_FINISHING_INDICATOR)) {
            removeLastCharFromInput();
            builder.isMultiLineFinishing(true);
        }
    }

    private void removeLastCharFromInput() {
        parsedPropertyInput = parsedPropertyInput.substring(0, parsedPropertyInput.length() - 1);
    }
}
