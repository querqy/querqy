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

import querqy.json.JsonProvider;
import querqy.rewrite.RuleParseException;
import querqy.rewriter.commonrules.rules.SkeletonComponentParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PropertySkeletonParser implements SkeletonComponentParser<Map<String, Object>> {

    private final JsonProvider jsonProvider;

    private final Map<String, Object> properties = new HashMap<>();

    private String content = null;
    private StringBuilder multiLineInput = null;
    private PropertySkeletonInput currentPropertySkeletonInput = null;

    private PropertySkeletonParser(final JsonProvider jsonProvider) {
        this.jsonProvider = Objects.requireNonNull(jsonProvider, "jsonProvider must not be null");
    }

    public static PropertySkeletonParser create(final JsonProvider jsonProvider) {
        return new PropertySkeletonParser(jsonProvider);
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public boolean isParsable() {
        if (content == null) {
            throw new IllegalStateException("Content must be set before calling isParsable()");
        }

        return isInMultiLineParsingMode() || content.startsWith(PropertySkeletonInputParser.PROPERTY_INDICATOR);
    }

    private boolean isInMultiLineParsingMode() {
        return multiLineInput != null;
    }

    public void parse() {
        parsePropertyInput();

        if (isInMultiLineParsingMode() || currentPropertySkeletonInput.isMultiLineInitiation()) {
            parseAsMultiLine();

        } else if (currentPropertySkeletonInput.isPropertyInitiation()) {
            parseAsSingleLine();

        } else {
            throw new RuleParseException(String.format("Cannot parse property %s", currentPropertySkeletonInput.getRawInput()));
        }
    }

    private void parsePropertyInput() {
        if (content == null) {
            throw new IllegalStateException("Content must be set before parsing");
        }

        final PropertySkeletonInputParser propertySkeletonInputParser = PropertySkeletonInputParser.of(content);
        currentPropertySkeletonInput = propertySkeletonInputParser.parse();
    }

    private void parseAsMultiLine() {
        if (multiLineInput == null) {
            multiLineInput = new StringBuilder();
        }

        appendMultiLineInput();

        if (currentPropertySkeletonInput.isMultiLineFinishing()) {
            finishMultiLineParsing();
        }
    }

    private void appendMultiLineInput() {
        multiLineInput.append(currentPropertySkeletonInput.getStrippedInput());
        multiLineInput.append("\n");
    }

    private void finishMultiLineParsing() {
        final Map<String, Object> additionalProperties = deserializeMultiLineInput();
        putAllProperties(additionalProperties);

        multiLineInput = null;
    }

    private Map<String, Object> deserializeMultiLineInput() {
        final String jsonString = multiLineInput.toString();
        return deserializeJsonString(jsonString);
    }

    private Map<String, Object> deserializeJsonString(final String jsonString) {
        final String normalized = LenientJsonNormalizer.normalize(jsonString);

        try {
            return jsonProvider.readMap(normalized);

        } catch (IOException e) {
            throw new RuleParseException(
                    String.format("Could not parse multiline properties for input %s", jsonString), e);
        }
    }

    private void putAllProperties(final Map<String, Object> additionalProperties) {
        additionalProperties.forEach(this::putProperty);
    }

    private void putProperty(final String key, final Object value) {
        if (properties.containsKey(key)) {
            throw new RuleParseException(
                    String.format("Error adding property [%s, %s]: Property is already set", key, value));
        }

        properties.put(key, value);
    }

    private void parseAsSingleLine() {
        final String propertyAsJsonString = "{" + currentPropertySkeletonInput.getStrippedInput() + "}";
        final Map<String, Object> additionalProperties = deserializeJsonString(propertyAsJsonString);
        putAllProperties(additionalProperties);
    }

    public boolean hasProperties() {
        return !properties.isEmpty();
    }

    public Map<String, Object> finish() {
        if (isInMultiLineParsingMode()) {
            throw new RuleParseException(String.format("Multiline properties input in incomplete %s", multiLineInput));
        }

        final Map<String, Object> propertiesToReturn = new HashMap<>(properties);

        content = null;
        properties.clear();

        return propertiesToReturn;
    }

    public static String toTextDefinition(final String key, final String value) {
        return "@" + key + ": " + value;
    }

    public static String toTextDefinition(final Map<String, Object> properties, final JsonProvider jsonProvider) {
        try {
            final String jsonString = jsonProvider.writeValueAsString(properties);
            return "@" + jsonString + "@";

        } catch (IOException e) {
            throw new RuleParseException("Could not transform properties to JSON", e);
        }
    }
}
