/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014 Querqy Contributors
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

public class CharSequenceUtilTest {

    @Test
    public void testNullDoesNotCauseExceptionInEquals() {
        assertTrue(CharSequenceUtil.equals(null, null));
        assertFalse(CharSequenceUtil.equals("", null));
        assertFalse(CharSequenceUtil.equals(null, ""));
    }
    
    @Test
    public void testEqualSubstring() throws Exception {
        assertFalse(CharSequenceUtil.equals("", "a"));
        assertFalse(CharSequenceUtil.equals("a", ""));
        assertFalse(CharSequenceUtil.equals("ab", "abc"));
        assertFalse(CharSequenceUtil.equals("abc", "ab"));
    }
    
    @Test
    public void testEqualStrings() throws Exception {
        assertTrue(CharSequenceUtil.equals("abc", "abc"));
        assertTrue(CharSequenceUtil.equals("", ""));
    }
    
    @Test
    public void testEqualIsCaseSensitive() throws Exception {
        assertFalse(CharSequenceUtil.equals("ABC", "abc"));
        assertFalse(CharSequenceUtil.equals("abc", "ABC"));
    }
    
    @Test
    public void testClassHandling() throws Exception {
        
        assertTrue(CharSequenceUtil.equals("abc", new LowerCaseCharSequence("ABC")));
        assertTrue(CharSequenceUtil.equals(new LowerCaseCharSequence("ABC"), "abc"));
        
        assertFalse(CharSequenceUtil.equals("abc", new Object() {@Override
        public String toString() {
            return "abc";
        }}));
        
    }

}
