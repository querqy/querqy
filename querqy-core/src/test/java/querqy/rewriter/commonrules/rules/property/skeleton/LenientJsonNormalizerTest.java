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
package querqy.rewriter.commonrules.rules.property.skeleton;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LenientJsonNormalizerTest {

    @Test
    public void testThat_strictJsonPassesThroughUnchanged() {
        final String json = "{\"a\":\"b\",\"c\":true,\"d\":1}";
        assertThat(LenientJsonNormalizer.normalize(json)).isEqualTo(json);
    }

    @Test
    public void testThat_unquotedKeysAreQuoted() {
        assertThat(LenientJsonNormalizer.normalize("{a: \"b\"}")).isEqualTo("{\"a\": \"b\"}");
    }

    @Test
    public void testThat_unquotedKeyWithWhitespaceBeforeColonIsQuoted() {
        assertThat(LenientJsonNormalizer.normalize("{a   : \"b\"}")).isEqualTo("{\"a\"   : \"b\"}");
    }

    @Test
    public void testThat_alreadyQuotedKeysAreUnaffected() {
        assertThat(LenientJsonNormalizer.normalize("{\"a\": \"b\"}")).isEqualTo("{\"a\": \"b\"}");
    }

    @Test
    public void testThat_bareJsonLiteralValuesAreNotQuoted() {
        assertThat(LenientJsonNormalizer.normalize("{\"a\": true, \"b\": null, \"c\": 1}"))
                .isEqualTo("{\"a\": true, \"b\": null, \"c\": 1}");
    }

    @Test
    public void testThat_singleQuotedStringsBecomeDoubleQuoted() {
        assertThat(LenientJsonNormalizer.normalize("{\"a\": 'b'}")).isEqualTo("{\"a\": \"b\"}");
    }

    @Test
    public void testThat_escapedSingleQuoteInsideSingleQuotedStringBecomesLiteral() {
        assertThat(LenientJsonNormalizer.normalize("{\"a\": 'it\\'s b'}")).isEqualTo("{\"a\": \"it's b\"}");
    }

    @Test
    public void testThat_literalDoubleQuoteInsideSingleQuotedStringIsEscaped() {
        assertThat(LenientJsonNormalizer.normalize("{\"a\": 'say \"hi\"'}"))
                .isEqualTo("{\"a\": \"say \\\"hi\\\"\"}");
    }

    @Test
    public void testThat_hashCommentsAreStripped() {
        assertThat(LenientJsonNormalizer.normalize("{\n  \"a\": \"b\" # a comment\n}"))
                .isEqualTo("{\n  \"a\": \"b\" \n}");
    }

    @Test
    public void testThat_hashInsideDoubleQuotedStringIsNotTreatedAsComment() {
        final String json = "{\"a\": \"value # not a comment\"}";
        assertThat(LenientJsonNormalizer.normalize(json)).isEqualTo(json);
    }

    @Test
    public void testThat_singleQuoteInsideDoubleQuotedStringIsUnaffected() {
        final String json = "{\"a\": \"it's fine\"}";
        assertThat(LenientJsonNormalizer.normalize(json)).isEqualTo(json);
    }

    @Test
    public void testThat_colonInsideStringValueDoesNotConfuseKeyDetection() {
        assertThat(LenientJsonNormalizer.normalize("{a: \"b: c\"}")).isEqualTo("{\"a\": \"b: c\"}");
    }

    @Test
    public void testThat_combinedLeniencyFeaturesAllNormalizeTogether() {
        final String lenient = "{\n"
                + "  outer: { # a comment here\n"
                + "    'inner': 'value with \"quotes\"'\n"
                + "  }\n"
                + "}";
        final String expected = "{\n"
                + "  \"outer\": { \n"
                + "    \"inner\": \"value with \\\"quotes\\\"\"\n"
                + "  }\n"
                + "}";
        assertThat(LenientJsonNormalizer.normalize(lenient)).isEqualTo(expected);
    }

    @Test
    public void testThat_escapedBackslashBeforeClosingDoubleQuoteDoesNotEatTheClosingQuote() {
        final String json = "{\"a\": \"ends with backslash\\\\\"}";
        assertThat(LenientJsonNormalizer.normalize(json)).isEqualTo(json);
    }
}
