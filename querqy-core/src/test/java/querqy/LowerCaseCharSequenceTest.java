/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2016 Querqy Contributors
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

/**
 * Created by rene on 02/11/2016.
 */
public class LowerCaseCharSequenceTest {

    @Test
    public void testThatToStringReturnsTheLowerCaseString() throws Exception {
        assertEquals("hello world!", new LowerCaseCharSequence("Hello WORLD!").toString());
    }

    @Test
    public void testThatCompareToIsCaseInsensitive() throws Exception {

        assertEquals(0, new LowerCaseCharSequence("Hello WORLD!").compareTo("hello world!"));

        LowerCaseCharSequence upperA = new LowerCaseCharSequence("ABC");
        LowerCaseCharSequence lowerA = new LowerCaseCharSequence("abc");

        LowerCaseCharSequence upperB = new LowerCaseCharSequence("BCD");
        LowerCaseCharSequence lowerB = new LowerCaseCharSequence("bcd");

        assertEquals(upperA.compareTo(upperB), lowerA.compareTo(lowerB));
        assertEquals(upperB.compareTo(upperA), lowerB.compareTo(lowerA));

    }

}
