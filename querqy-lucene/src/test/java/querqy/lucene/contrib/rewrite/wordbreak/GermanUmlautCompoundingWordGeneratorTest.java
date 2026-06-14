/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2022 Querqy Contributors
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
package querqy.lucene.contrib.rewrite.wordbreak;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

public class GermanUmlautCompoundingWordGeneratorTest {
    @Test
    public void replaceUmlaut_NoSuffixAdding() {
        final GermanUmlautCompoundingWordGenerator generator = new GermanUmlautCompoundingWordGenerator();
        final Optional<CharSequence> res = generator.generateModifier("gans");
        assertThat(res.get(), Matchers.is("gäns"));
    }

    @Test
    public void replaceUmlaut_AddSuffix() {
        final GermanUmlautCompoundingWordGenerator generator = new GermanUmlautCompoundingWordGenerator("e");
        final Optional<CharSequence> res = generator.generateModifier("gans");
        assertThat(res.get(), Matchers.is("gänse"));
    }

    @Test
    public void noUmlautsToReplace() {
        final GermanUmlautCompoundingWordGenerator generator = new GermanUmlautCompoundingWordGenerator("e");
        final Optional<CharSequence> res = generator.generateModifier("finger");
        assertFalse(res.isPresent());
    }
}