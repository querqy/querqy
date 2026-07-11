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
package querqy.rewriter.wordbreak;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class OptionalModifierConfigTest {

    @Test
    public void testDisabledConstantIsNoneWithNeutralBoost() {
        assertEquals(OptionalModifierPosition.NONE, OptionalModifierConfig.DISABLED.position());
        assertEquals(1f, OptionalModifierConfig.DISABLED.boost(), 0f);
    }

    @Test
    public void testNonNeutralBoostIsAcceptedForFirst() {
        final OptionalModifierConfig config = new OptionalModifierConfig(OptionalModifierPosition.FIRST, 2.5f);
        assertEquals(OptionalModifierPosition.FIRST, config.position());
        assertEquals(2.5f, config.boost(), 0f);
    }

    @Test
    public void testNonNeutralBoostIsAcceptedForLast() {
        final OptionalModifierConfig config = new OptionalModifierConfig(OptionalModifierPosition.LAST, 0.5f);
        assertEquals(OptionalModifierPosition.LAST, config.position());
        assertEquals(0.5f, config.boost(), 0f);
    }

    @Test
    public void testNeutralBoostIsAcceptedForNone() {
        final OptionalModifierConfig config = new OptionalModifierConfig(OptionalModifierPosition.NONE, 1f);
        assertEquals(OptionalModifierPosition.NONE, config.position());
    }

    @Test
    public void testNonNeutralBoostIsRejectedForNone() {
        try {
            new OptionalModifierConfig(OptionalModifierPosition.NONE, 2f);
            fail("expected IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testNullPositionIsRejected() {
        try {
            new OptionalModifierConfig(null, 1f);
            fail("expected IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            // expected
        }
    }
}
