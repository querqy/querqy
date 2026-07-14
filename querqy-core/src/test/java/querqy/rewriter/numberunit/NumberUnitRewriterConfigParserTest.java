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

import org.junit.Test;
import querqy.rewriter.numberunit.model.NumberUnitDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NumberUnitRewriterConfigParserTest {

    private static final String BASE_PATH = "numberunit/";

    @Test
    public void testFullConfig() throws IOException {
        final ParsedNumberUnitConfig parsedConfig = NumberUnitRewriterConfigParser.parse(
                readConfig("number-unit-full-config.json"));

        assertThat(parsedConfig.scaleForLinearFunctions).isEqualTo(1001);

        final List<NumberUnitDefinition> numberUnitDefinitions = parsedConfig.numberUnitDefinitions;
        assertThat(numberUnitDefinitions).hasSize(1);

        final NumberUnitDefinition numberUnitDefinition = numberUnitDefinitions.get(0);

        assertThat(numberUnitDefinition.unitDefinitions).hasSize(1);
        assertThat(numberUnitDefinition.unitDefinitions.get(0).term).isEqualTo("term");
        assertThat(numberUnitDefinition.unitDefinitions.get(0).multiplier.doubleValue()).isEqualTo(1002);

        assertThat(numberUnitDefinition.fields).hasSize(1);
        assertThat(numberUnitDefinition.fields.get(0).fieldName).isEqualTo("fieldName");
        assertThat(numberUnitDefinition.fields.get(0).scale).isEqualTo(1003);

        assertThat(numberUnitDefinition.maxScoreForExactMatch.doubleValue()).isEqualTo(1004);
        assertThat(numberUnitDefinition.minScoreAtUpperBoundary.doubleValue()).isEqualTo(1005);
        assertThat(numberUnitDefinition.minScoreAtLowerBoundary.doubleValue()).isEqualTo(1006);
        assertThat(numberUnitDefinition.additionalScoreForExactMatch.doubleValue()).isEqualTo(1007);

        assertThat(numberUnitDefinition.boostPercentageUpperBoundary.doubleValue()).isEqualTo(1008);
        assertThat(numberUnitDefinition.boostPercentageLowerBoundary.doubleValue()).isEqualTo(1009);
        assertThat(numberUnitDefinition.boostPercentageUpperBoundaryExactMatch.doubleValue()).isEqualTo(1010);
        assertThat(numberUnitDefinition.boostPercentageLowerBoundaryExactMatch.doubleValue()).isEqualTo(1011);

        assertThat(numberUnitDefinition.filterPercentageUpperBoundary.doubleValue()).isEqualTo(1012);
        assertThat(numberUnitDefinition.filterPercentageLowerBoundary.doubleValue()).isEqualTo(1013);
    }

    @Test
    public void testMinimalConfigAppliesDefaults() throws IOException {
        final ParsedNumberUnitConfig parsedConfig = NumberUnitRewriterConfigParser.parse(
                readConfig("number-unit-minimal-config.json"));

        assertThat(parsedConfig.scaleForLinearFunctions).isEqualTo(5);

        final NumberUnitDefinition numberUnitDefinition = parsedConfig.numberUnitDefinitions.get(0);

        assertThat(numberUnitDefinition.unitDefinitions.get(0).term).isEqualTo("term");
        assertThat(numberUnitDefinition.unitDefinitions.get(0).multiplier.intValue()).isEqualTo(1);

        assertThat(numberUnitDefinition.fields.get(0).fieldName).isEqualTo("fieldName");
        assertThat(numberUnitDefinition.fields.get(0).scale).isEqualTo(0);

        assertThat(numberUnitDefinition.maxScoreForExactMatch.intValue()).isEqualTo(200);
        assertThat(numberUnitDefinition.minScoreAtUpperBoundary.intValue()).isEqualTo(100);
        assertThat(numberUnitDefinition.minScoreAtLowerBoundary.intValue()).isEqualTo(100);
        assertThat(numberUnitDefinition.additionalScoreForExactMatch.intValue()).isEqualTo(100);

        assertThat(numberUnitDefinition.boostPercentageUpperBoundary.intValue()).isEqualTo(20);
        assertThat(numberUnitDefinition.boostPercentageLowerBoundary.intValue()).isEqualTo(20);
        assertThat(numberUnitDefinition.boostPercentageUpperBoundaryExactMatch.intValue()).isEqualTo(0);
        assertThat(numberUnitDefinition.boostPercentageLowerBoundaryExactMatch.intValue()).isEqualTo(0);

        assertThat(numberUnitDefinition.filterPercentageUpperBoundary.intValue()).isEqualTo(20);
        assertThat(numberUnitDefinition.filterPercentageLowerBoundary.intValue()).isEqualTo(20);
    }

    @Test
    public void testParseThrowsOnInvalidConfig() throws IOException {
        final String config = readConfig("number-unit-invalid-config.json");
        assertThatThrownBy(() -> NumberUnitRewriterConfigParser.parse(config))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testValidateReturnsEmptyForValidConfig() throws IOException {
        assertThat(NumberUnitRewriterConfigParser.validate(readConfig("number-unit-full-config.json"))).isEmpty();
        assertThat(NumberUnitRewriterConfigParser.validate(readConfig("number-unit-minimal-config.json"))).isEmpty();
    }

    @Test
    public void testValidateReturnsErrorForInvalidConfig() throws IOException {
        assertThat(NumberUnitRewriterConfigParser.validate(readConfig("number-unit-invalid-config.json")))
                .isNotEmpty();
    }

    @Test
    public void testValidateReturnsErrorForDuplicateUnits() throws IOException {
        assertThat(NumberUnitRewriterConfigParser.validate(readConfig("number-unit-duplicate-units-config.json")))
                .containsExactly("Units must only be defined once per NumberUnitDefinition");
    }

    @Test
    public void testValidateReturnsErrorForMalformedJson() {
        assertThat(NumberUnitRewriterConfigParser.validate("not json")).isNotEmpty();
    }

    private static String readConfig(final String fileName) {
        try (final InputStream inputStream = NumberUnitRewriterConfigParserTest.class.getClassLoader()
                .getResourceAsStream(BASE_PATH + fileName)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
