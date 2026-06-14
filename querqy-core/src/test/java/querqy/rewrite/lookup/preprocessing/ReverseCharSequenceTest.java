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
import querqy.rewrite.lookup.preprocessing.ReverseCharSequence;

import static org.junit.Assert.*;

public class ReverseCharSequenceTest {

    final static ReverseCharSequence TEST_SEQ = new ReverseCharSequence("1234567890");


    @Test
    public void testToString() {
        assertEquals("0987654321", TEST_SEQ.toString());
    }

    @Test
    public void testSubSequence() {
        assertEquals("987", TEST_SEQ.subSequence(1, 4).toString());
        assertEquals("0987654321", TEST_SEQ.subSequence(0, 10).toString());
    }

    @Test
    public void testCharAt() {
        assertEquals('0', TEST_SEQ.charAt(0));
        assertEquals('6', TEST_SEQ.charAt(4));
        assertEquals('1', TEST_SEQ.charAt(9));
    }

    @Test
    public void testEmptySequence() {
        assertEquals("", new ReverseCharSequence("").toString());
        assertEquals(0, new ReverseCharSequence("").length());
    }

    @Test
    public void testEquals() {
        assertEquals(new ReverseCharSequence("abcd"), new ReverseCharSequence("abcd"));
    }

    @Test
    public void testHashCode() {
        assertEquals(new ReverseCharSequence("abcd").hashCode(),
                new ReverseCharSequence("abcd").hashCode());
        assertNotEquals(new ReverseCharSequence("abcd").hashCode(),
                new ReverseCharSequence("d").hashCode());
    }

}