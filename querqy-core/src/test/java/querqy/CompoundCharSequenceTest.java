/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2015 Querqy Contributors
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


import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author rene
 * @author jope
 *
 */
public class CompoundCharSequenceTest {
    
    @Test
    public void testCharAtWithoutSeparator() {
        CompoundCharSequence seq = new CompoundCharSequence(Arrays.asList("cde-fgh", "-"));
        assertEquals(8, seq.length());
        for (int i = 0; i < 8; i++) {
            assertEquals("cde-fgh-".charAt(i), seq.charAt(i));
        }
    }
    
    @Test
    public void testCharAtWithSeparator() {
        CompoundCharSequence seq = new CompoundCharSequence(" ", Arrays.asList("cde", "-fgh", "-", "ijklm"));
        assertEquals(16, seq.length());
        for (int i = 0; i < 16; i++) {
            assertEquals("cde -fgh - ijklm".charAt(i), seq.charAt(i));
        }
    }

    @Test
    public void testCharAtOutOfOrderAccess() {
        CompoundCharSequence seq = new CompoundCharSequence(" ", Arrays.asList("cde", "-fgh", "-", "ijklm"));
        String expected = "cde -fgh - ijklm";
        assertEquals(expected.length(), seq.length());

        // deliberately jumps back and forth across segment boundaries instead of walking left-to-right,
        // to exercise charAt()'s segment-index cache under non-sequential access
        int[] order = {15, 0, 7, 3, 15, 8, 1, 14, 4, 0, 9, 6, 2, 10, 5, 11, 12, 13};
        for (int i : order) {
            assertEquals("mismatch at index " + i, expected.charAt(i), seq.charAt(i));
        }
    }

    @Test
    public void testSubSequenceOnlyOnePart() {
        ComparableCharSequence seq;

        seq = new CompoundCharSequence(Collections.singletonList("abcdefg"));
        for (int i = 0; i < seq.length(); i++) {
            assertEquals("abcdefg".charAt(i), seq.charAt(i));
        }

        seq = seq.subSequence(0, 7);
        for (int i = 0; i < seq.length(); i++) {
            assertEquals("abcdefg".charAt(i), seq.charAt(i));
        }

        seq = seq.subSequence(2, 5);
        for (int i = 0; i < seq.length(); i++) {
            assertEquals("cde".charAt(i), seq.charAt(i));
        }

        seq = seq.subSequence(3, 3);
        assertEquals(ComparableCharSequenceWrapper.EMPTY_SEQUENCE, seq);
    }

    @Test
    public void testSubSequence() {
        ComparableCharSequence seq;

        seq = new CompoundCharSequence(Arrays.asList("abcd", "efgh", "ijk"));
        for (int i = 0; i < seq.length(); i++) {
            assertEquals("abcdefghijk".charAt(i), seq.charAt(i));
        }

        seq = seq.subSequence(2, 10);
        for (int i = 0; i < seq.length(); i++) {
            assertEquals("cdefghij".charAt(i), seq.charAt(i));
        }

        seq = seq.subSequence(2, 8);
        for (int i = 0; i < seq.length(); i++) {
            assertEquals("efghij".charAt(i), seq.charAt(i));
        }

        seq = seq.subSequence(0, 4);
        for (int i = 0; i < seq.length(); i++) {
            assertEquals("efgh".charAt(i), seq.charAt(i));
        }

        seq = seq.subSequence(0, 4);
        for (int i = 0; i < seq.length(); i++) {
            assertEquals("efgh".charAt(i), seq.charAt(i));
        }

        assertTrue(new ComparableCharSequenceWrapper("efgh").equals(seq));

        assertEquals(new ComparableCharSequenceWrapper("efgh").hashCode(), seq.hashCode());

        assertThat(new ComparableCharSequenceWrapper("efhh").compareTo(seq)).isPositive();
        assertThat(new ComparableCharSequenceWrapper("efgg").compareTo(seq)).isNegative();
        assertThat(new ComparableCharSequenceWrapper("efgh").compareTo(seq)).isZero();

        seq = seq.subSequence(3, 3);
        assertEquals(ComparableCharSequenceWrapper.EMPTY_SEQUENCE, seq);
    }
}
