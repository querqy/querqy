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

import static org.junit.Assert.*;

import org.junit.Test;

public class SimpleComparableCharSequenceTest {

    @Test
    public void testCharAt() throws Exception {
        SimpleComparableCharSequence seq = new SimpleComparableCharSequence("cde-fgh-".toCharArray(), 0, 8);
        assertEquals(8, seq.length());
        for (int i = 0; i < 8; i++) {
            assertEquals("cde-fgh-".charAt(i), seq.charAt(i));
        }
        
    }
    
    
    @Test
    public void testSubSequence() throws Exception {
        SimpleComparableCharSequence seq = new SimpleComparableCharSequence("abcd".toCharArray(), 0, 4);
        assertEquals("a", seq.subSequence(0, 1).toString());
        assertEquals("ab", seq.subSequence(0, 2).toString());
        assertEquals("bc", seq.subSequence(1, 3).toString());
        assertEquals("d", seq.subSequence(3, 4).toString());
        assertEquals("", seq.subSequence(0, 0).toString());
        assertEquals("", seq.subSequence(1, 1).toString());
        assertEquals("", seq.subSequence(4, 4).toString());
    }

}
