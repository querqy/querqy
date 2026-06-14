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
package querqy.rewrite.commonrules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static querqy.rewrite.commonrules.EscapeUtil.unescape;

import org.junit.Test;

public class EscapeUtilTest {

    @Test
    public void testUnescape() {
        assertThat(unescape("\\*")).isEqualTo("*");
        assertThat(unescape("\\\"")).isEqualTo("\"");
        assertThat(unescape("\\#")).isEqualTo("#");
        assertThat(unescape("\\\\")).isEqualTo("\\");
        assertThat(unescape("\\\\*")).isEqualTo("\\*");
        assertThatThrownBy(() -> unescape("\\1")).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(unescape("a\\\\")).isEqualTo("a\\");
        assertThat(unescape("a\\*")).isEqualTo("a*");
        assertThat(unescape("\\")).isEqualTo("");
    }

}
