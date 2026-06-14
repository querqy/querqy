/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2023 Querqy Contributors
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
package querqy.rewrite.lookup.preprocessing;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GermanUmlautPreprocessorTest {

    @Test
    public void testThat_aeIsReplaced_atTheBeginning() {
        final GermanUmlautPreprocessor preprocessor = GermanUmlautPreprocessor.create();
        assertThat(preprocessor.process("aerger"))
                .isEqualTo("ärger");
    }

    @Test
    public void testThat_aeIsReplaced_atTheEnd() {
        final GermanUmlautPreprocessor preprocessor = GermanUmlautPreprocessor.create();
        assertThat(preprocessor.process("mariae"))
                .isEqualTo("mariä");
    }

    @Test
    public void testThat_aeIsReplaced_inTheMiddle() {
        final GermanUmlautPreprocessor preprocessor = GermanUmlautPreprocessor.create();
        assertThat(preprocessor.process("maehne"))
                .isEqualTo("mähne");
    }

    @Test
    public void testThat_aeIsReplaced_multipleTimes() {
        final GermanUmlautPreprocessor preprocessor = GermanUmlautPreprocessor.create();
        assertThat(preprocessor.process("aehaehae"))
                .isEqualTo("ähähä");
    }

    @Test
    public void testThat_oeIsReplaced_atTheBeginning() {
        final GermanUmlautPreprocessor preprocessor = GermanUmlautPreprocessor.create();
        assertThat(preprocessor.process("oeh"))
                .isEqualTo("öh");
    }

    @Test
    public void testThat_oeIsReplaced_atTheEnd() {
        final GermanUmlautPreprocessor preprocessor = GermanUmlautPreprocessor.create();
        assertThat(preprocessor.process("hoe"))
                .isEqualTo("hö");
    }

    @Test
    public void testThat_oeIsReplaced_inTheMiddle() {
        final GermanUmlautPreprocessor preprocessor = GermanUmlautPreprocessor.create();
        assertThat(preprocessor.process("hoeh"))
                .isEqualTo("höh");
    }

    @Test
    public void testThat_oeIsReplaced_multipleTimes() {
        final GermanUmlautPreprocessor preprocessor = GermanUmlautPreprocessor.create();
        assertThat(preprocessor.process("oehoehoe"))
                .isEqualTo("öhöhö");
    }

    @Test
    public void testThat_ueIsReplaced_atTheBeginning() {
        final GermanUmlautPreprocessor preprocessor = GermanUmlautPreprocessor.create();
        assertThat(preprocessor.process("ueh"))
                .isEqualTo("üh");
    }

    @Test
    public void testThat_ueIsReplaced_atTheEnd() {
        final GermanUmlautPreprocessor preprocessor = GermanUmlautPreprocessor.create();
        assertThat(preprocessor.process("hue"))
                .isEqualTo("hü");
    }

    @Test
    public void testThat_ueIsReplaced_inTheMiddle() {
        final GermanUmlautPreprocessor preprocessor = GermanUmlautPreprocessor.create();
        assertThat(preprocessor.process("hueh"))
                .isEqualTo("hüh");
    }

    @Test
    public void testThat_ueIsReplaced_multipleTimes() {
        final GermanUmlautPreprocessor preprocessor = GermanUmlautPreprocessor.create();
        assertThat(preprocessor.process("uehuehue"))
                .isEqualTo("ühühü");
    }

    @Test
    public void testThat_ueIsNotReplaced_ifPrependedByA() {
        final GermanUmlautPreprocessor preprocessor = GermanUmlautPreprocessor.create();
        assertThat(preprocessor.process("uehaue"))
                .isEqualTo("ühaue");
    }

    @Test
    public void testThat_aouAreNotReplaced_ifNotFollowedByE() {
        final GermanUmlautPreprocessor preprocessor = GermanUmlautPreprocessor.create();
        assertThat(preprocessor.process("aouhahohu"))
                .isEqualTo("aouhahohu");
    }

    @Test
    public void testThat_nothingIsThrown_forEAtDifferentPositions() {
        final GermanUmlautPreprocessor preprocessor = GermanUmlautPreprocessor.create();
        assertThat(preprocessor.process("eee"))
                .isEqualTo("eee");
    }

}
