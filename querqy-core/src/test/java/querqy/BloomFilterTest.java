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
package querqy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BloomFilterTest {

    @Test
    public void containsReturnsTrueForAddedTerm() {
        final BloomFilter bf = new BloomFilter(128, 3);
        bf.add("running");
        assertTrue(bf.contains("running"));
    }

    @Test
    public void containsReturnsFalseForTermNotAdded() {
        final BloomFilter bf = new BloomFilter(128, 3);
        bf.add("running");
        assertFalse(bf.contains("shoes"));
    }

    @Test
    public void containsAcceptsCharSequence() {
        final BloomFilter bf = new BloomFilter(128, 3);
        bf.add("running");
        final CharSequence cs = new StringBuilder("running");
        assertTrue(bf.contains(cs));
    }

    @Test
    public void toHexAndFromHexRoundtrip() {
        final BloomFilter original = new BloomFilter(128, 3);
        original.add("running");
        original.add("shoes");

        final BloomFilter restored = BloomFilter.fromHex(original.toHex(), 3);

        assertTrue(restored.contains("running"));
        assertTrue(restored.contains("shoes"));
        assertFalse(restored.contains("hiking"));
    }

    @Test
    public void toHexProducesCorrectLength() {
        final BloomFilter bf = new BloomFilter(128, 3);
        assertEquals(32, bf.toHex().length()); // 128 bits / 4 bits per hex char
    }

    @Test
    public void fromHexRestoresBitCount() {
        final BloomFilter bf = BloomFilter.fromHex("0000000000000000ffffffffffffffff", 2);
        assertEquals(128, bf.getBits());
        assertEquals(2, bf.getHashFunctions());
    }

    @Test
    public void saturateMakesContainsTrueForAnyTerm() {
        final BloomFilter bf = new BloomFilter(64, 3);
        bf.saturate();
        assertTrue(bf.contains("running"));
        assertTrue(bf.contains("shoes"));
        assertTrue(bf.contains("anything"));
    }

    @Test
    public void orProducesUnionOfBothFilters() {
        final BloomFilter a = new BloomFilter(128, 3);
        a.add("running");

        final BloomFilter b = new BloomFilter(128, 3);
        b.add("shoes");

        final BloomFilter union = BloomFilter.or(a, b);

        assertTrue(union.contains("running"));
        assertTrue(union.contains("shoes"));
    }

    @Test
    public void orDoesNotModifyOperands() {
        final BloomFilter a = new BloomFilter(128, 3);
        a.add("running");

        final BloomFilter b = new BloomFilter(128, 3);
        b.add("shoes");

        BloomFilter.or(a, b);

        assertFalse(a.contains("shoes"));
        assertFalse(b.contains("running"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsZeroBits() {
        new BloomFilter(0, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsBitsNotMultipleOf64() {
        new BloomFilter(100, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromHexRejectsHexLengthNotMultipleOf16() {
        BloomFilter.fromHex("0000000000000", 3);
    }
}
