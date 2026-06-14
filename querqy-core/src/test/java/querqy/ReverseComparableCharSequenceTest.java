/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Querqy Contributors
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
import static org.junit.Assert.assertTrue;

public class ReverseComparableCharSequenceTest {

    @Test
    public void testCompareTo() {
        ComparableCharSequence seq;
        ReverseComparableCharSequence revSeq;

        seq = seq("gfedcba");
        revSeq = new ReverseComparableCharSequence("abcdefg");
        assertEquals(0, revSeq.compareTo(seq));

        seq = seq("gfedcba");
        revSeq = new ReverseComparableCharSequence("abcddfg");
        assertTrue(revSeq.compareTo(seq) < 0);

        seq = seq("gfedcba");
        revSeq = new ReverseComparableCharSequence("abcdffg");
        assertTrue(revSeq.compareTo(seq) > 0);
    }

    @Test
    public void testSeqComparison() {
        ComparableCharSequence seq = seq("gfedcba");
        ReverseComparableCharSequence revSeq = new ReverseComparableCharSequence("abcdefg");

        assertEquals(seq.length(), revSeq.length());
        assertTrue(revSeq.equals(seq));
        assertEquals(seq.hashCode(), revSeq.hashCode());
    }

    @Test
    public void testStringComparison() {
        String str = "gfedcba";
        ReverseComparableCharSequence revSeq = new ReverseComparableCharSequence("abcdefg");

        assertEquals(str.length(), revSeq.length());
        assertEquals(str.hashCode(), revSeq.toString().hashCode());
        assertTrue(revSeq.equals(str));
        assertTrue(str.equals(revSeq.toString()));
    }

    @Test
    public void testSubSequence() {
        ReverseComparableCharSequence revSeq = new ReverseComparableCharSequence("abcdefg");

        assertEquals(seq("gfe"), revSeq.subSequence(0, 3));
        assertEquals(seq("gfedcba"), revSeq.subSequence(0, 7));
        assertEquals(seq("dcba"), revSeq.subSequence(3, 7));
        assertEquals(seq("dc"), revSeq.subSequence(3, 5));
        assertEquals(seq("edc"), revSeq.subSequence(1, 6).subSequence(1, 4));
    }

    private ComparableCharSequence seq(String str) {
        return new ComparableCharSequenceWrapper(str);
    }

    @Test
    public void testCharAt() {
        ReverseComparableCharSequence revSeq = new ReverseComparableCharSequence("abcdefg");

        assertEquals('g', revSeq.charAt(0));
        assertEquals('f', revSeq.charAt(1));
        assertEquals('e', revSeq.charAt(2));
        assertEquals('d', revSeq.charAt(3));
        assertEquals('c', revSeq.charAt(4));
        assertEquals('b', revSeq.charAt(5));
        assertEquals('a', revSeq.charAt(6));
    }
}
