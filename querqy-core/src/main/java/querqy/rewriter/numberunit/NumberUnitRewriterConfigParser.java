/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2026 Querqy Contributors
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
package querqy.rewriter.numberunit;

import com.fasterxml.jackson.databind.ObjectMapper;
import querqy.rewriter.numberunit.NumberUnitConfigObject.NumberUnitDefinitionObject;
import querqy.rewriter.numberunit.model.FieldDefinition;
import querqy.rewriter.numberunit.model.NumberUnitDefinition;
import querqy.rewriter.numberunit.model.UnitDefinition;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Parses the NumberUnitRewriter's JSON configuration into core's {@link NumberUnitDefinition} model,
 * applying the rewriter's default values and validation rules.
 */
class NumberUnitRewriterConfigParser {

    static final String EXCEPTION_MESSAGE = "NumberUnitRewriter not properly configured. " +
            "At least one unit and one field need to be properly defined, e. g. \n" +
            "{\n" +
            "  \"numberUnitDefinitions\": [\n" +
            "    {\n" +
            "      \"units\": [ { \"term\": \"cm\" } ],\n" +
            "      \"fields\": [ { \"fieldName\": \"weight\" } ]\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";

    private static final int DEFAULT_UNIT_MULTIPLIER = 1;

    private static final int DEFAULT_SCALE_FOR_LINEAR_FUNCTIONS = 5;
    private static final int DEFAULT_FIELD_SCALE = 0;

    private static final float DEFAULT_BOOST_MAX_SCORE_FOR_EXACT_MATCH = 200;
    private static final float DEFAULT_BOOST_MIN_SCORE_AT_UPPER_BOUNDARY = 100;
    private static final float DEFAULT_BOOST_MIN_SCORE_AT_LOWER_BOUNDARY = 100;
    private static final float DEFAULT_BOOST_ADDITIONAL_SCORE_FOR_EXACT_MATCH = 100;

    private static final float DEFAULT_BOOST_PERCENTAGE_UPPER_BOUNDARY = 20;
    private static final float DEFAULT_BOOST_PERCENTAGE_LOWER_BOUNDARY = 20;
    private static final float DEFAULT_BOOST_PERCENTAGE_UPPER_BOUNDARY_EXACT_MATCH = 0;
    private static final float DEFAULT_BOOST_PERCENTAGE_LOWER_BOUNDARY_EXACT_MATCH = 0;

    private static final float DEFAULT_FILTER_PERCENTAGE_LOWER_BOUNDARY = 20;
    private static final float DEFAULT_FILTER_PERCENTAGE_UPPER_BOUNDARY = 20;

    private NumberUnitRewriterConfigParser() {
    }

    /**
     * @throws IOException if {@code config} is not valid JSON
     * @throws IllegalArgumentException if the parsed config does not satisfy the rewriter's
     *         validation rules (e.g. missing unit/field definitions, blank term/fieldName)
     */
    static ParsedNumberUnitConfig parse(final String config) throws IOException {
        final NumberUnitConfigObject configObject = readConfigObject(config);
        final int scale = getOrDefaultInt(configObject::getScaleForLinearFunctions,
                DEFAULT_SCALE_FOR_LINEAR_FUNCTIONS);
        return new ParsedNumberUnitConfig(scale, parseNumberUnitDefinitions(configObject));
    }

    /**
     * @return a list of human-readable error messages; empty if {@code config} is valid
     */
    static List<String> validate(final String config) {
        try {
            final List<NumberUnitDefinition> numberUnitDefinitions =
                    parseNumberUnitDefinitions(readConfigObject(config));

            return numberUnitDefinitions.stream()
                    .filter(NumberUnitRewriterConfigParser::hasDuplicateUnitDefinition)
                    .findFirst()
                    .<List<String>>map(def -> Collections.singletonList(
                            "Units must only be defined once per NumberUnitDefinition"))
                    .orElse(Collections.emptyList());

        } catch (final IOException | IllegalArgumentException e) {
            return Collections.singletonList(e.getMessage());
        }
    }

    private static boolean hasDuplicateUnitDefinition(final NumberUnitDefinition numberUnitDefinition) {
        final Set<String> observedUnits = new HashSet<>();
        for (final UnitDefinition unitDefinition : numberUnitDefinition.unitDefinitions) {
            if (!observedUnits.add(unitDefinition.term)) {
                return true;
            }
        }
        return false;
    }

    private static NumberUnitConfigObject readConfigObject(final String config) throws IOException {
        return new ObjectMapper().readValue(config, NumberUnitConfigObject.class);
    }

    private static List<NumberUnitDefinition> parseNumberUnitDefinitions(final NumberUnitConfigObject configObject) {

        final List<NumberUnitDefinitionObject> numberUnitDefinitionObjects = configObject.getNumberUnitDefinitions();

        if (numberUnitDefinitionObjects == null || numberUnitDefinitionObjects.isEmpty()) {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE);
        }

        return numberUnitDefinitionObjects.stream()
                .map(NumberUnitRewriterConfigParser::parseNumberUnitDefinition)
                .collect(Collectors.toList());
    }

    private static NumberUnitDefinition parseNumberUnitDefinition(final NumberUnitDefinitionObject defObj) {

        final NumberUnitDefinition.Builder builder = NumberUnitDefinition.builder()
                .addUnits(parseUnitDefinitions(defObj))
                .addFields(parseFieldDefinitions(defObj));

        final NumberUnitConfigObject.BoostObject boost = defObj.getBoost() != null
                ? defObj.getBoost()
                : new NumberUnitConfigObject.BoostObject();

        builder
                .setMaxScoreForExactMatch(getOrDefaultBigDecimalForFloat(
                        boost::getMaxScoreForExactMatch, DEFAULT_BOOST_MAX_SCORE_FOR_EXACT_MATCH))
                .setMinScoreAtUpperBoundary(getOrDefaultBigDecimalForFloat(
                        boost::getMinScoreAtUpperBoundary, DEFAULT_BOOST_MIN_SCORE_AT_UPPER_BOUNDARY))
                .setMinScoreAtLowerBoundary(getOrDefaultBigDecimalForFloat(
                        boost::getMinScoreAtLowerBoundary, DEFAULT_BOOST_MIN_SCORE_AT_LOWER_BOUNDARY))
                .setAdditionalScoreForExactMatch(getOrDefaultBigDecimalForFloat(
                        boost::getAdditionalScoreForExactMatch, DEFAULT_BOOST_ADDITIONAL_SCORE_FOR_EXACT_MATCH))
                .setBoostPercentageUpperBoundary(getOrDefaultBigDecimalForFloat(
                        boost::getPercentageUpperBoundary, DEFAULT_BOOST_PERCENTAGE_UPPER_BOUNDARY))
                .setBoostPercentageLowerBoundary(getOrDefaultBigDecimalForFloat(
                        boost::getPercentageLowerBoundary, DEFAULT_BOOST_PERCENTAGE_LOWER_BOUNDARY))
                .setBoostPercentageUpperBoundaryExactMatch(getOrDefaultBigDecimalForFloat(
                        boost::getPercentageUpperBoundaryExactMatch,
                        DEFAULT_BOOST_PERCENTAGE_UPPER_BOUNDARY_EXACT_MATCH))
                .setBoostPercentageLowerBoundaryExactMatch(getOrDefaultBigDecimalForFloat(
                        boost::getPercentageLowerBoundaryExactMatch,
                        DEFAULT_BOOST_PERCENTAGE_LOWER_BOUNDARY_EXACT_MATCH));

        final NumberUnitConfigObject.FilterObject filter = defObj.getFilter() != null
                ? defObj.getFilter()
                : new NumberUnitConfigObject.FilterObject();

        builder
                .setFilterPercentageUpperBoundary(getOrDefaultBigDecimalForFloat(
                        filter::getPercentageUpperBoundary, DEFAULT_FILTER_PERCENTAGE_UPPER_BOUNDARY))
                .setFilterPercentageLowerBoundary(getOrDefaultBigDecimalForFloat(
                        filter::getPercentageLowerBoundary, DEFAULT_FILTER_PERCENTAGE_LOWER_BOUNDARY));

        return builder.build();
    }

    private static List<UnitDefinition> parseUnitDefinitions(final NumberUnitDefinitionObject numberUnitDefinitionObject) {
        final List<NumberUnitConfigObject.UnitObject> unitObjects = numberUnitDefinitionObject.getUnits();
        if (unitObjects == null || unitObjects.isEmpty()) {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE);
        }

        return unitObjects.stream()
                .peek(unitObject -> {
                    if (isBlank(unitObject.getTerm())) {
                        throw new IllegalArgumentException("Unit definition requires a term to be defined");
                    }
                })
                .map(unitObject -> new UnitDefinition(
                        unitObject.getTerm(),
                        getOrDefaultBigDecimalForFloat(unitObject::getMultiplier, DEFAULT_UNIT_MULTIPLIER)))
                .collect(Collectors.toList());
    }

    private static List<FieldDefinition> parseFieldDefinitions(final NumberUnitDefinitionObject numberUnitDefinitionObject) {
        final List<NumberUnitConfigObject.FieldObject> fieldObjects = numberUnitDefinitionObject.getFields();
        if (fieldObjects == null || fieldObjects.isEmpty()) {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE);
        }

        return fieldObjects.stream()
                .peek(fieldObject -> {
                    if (isBlank(fieldObject.getFieldName())) {
                        throw new IllegalArgumentException("Field definition requires a fieldName to be defined");
                    }
                })
                .map(fieldObject -> new FieldDefinition(
                        fieldObject.getFieldName(),
                        getOrDefaultInt(fieldObject::getScale, DEFAULT_FIELD_SCALE)))
                .collect(Collectors.toList());
    }

    private static BigDecimal getOrDefaultBigDecimalForFloat(final Supplier<Float> supplier, final float defaultValue) {
        final Float value = supplier.get();
        return value != null ? BigDecimal.valueOf(value) : BigDecimal.valueOf(defaultValue);
    }

    private static int getOrDefaultInt(final Supplier<Integer> supplier, final int defaultValue) {
        final Integer value = supplier.get();
        return value != null ? value : defaultValue;
    }

    private static boolean isBlank(final CharSequence cs) {
        if (cs == null || cs.length() == 0) {
            return true;
        }
        for (int i = 0; i < cs.length(); i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
