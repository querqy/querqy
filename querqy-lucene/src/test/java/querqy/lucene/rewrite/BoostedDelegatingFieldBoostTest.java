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
package querqy.lucene.rewrite;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class BoostedDelegatingFieldBoostTest {

    @Test
    public void shouldDoProperToString() {
        assertThat(new ConstantFieldBoost(666f).toString("title"), 
            is("^ConstantFieldBoost(title^666.0)"));
        assertThat(new BoostedDelegatingFieldBoost(new ConstantFieldBoost(666f), 2f).toString("title"), 
            is("^BoostedDelegatingFieldBoost(^ConstantFieldBoost(title^666.0)^2.0)"));
    }

    @Test
    public void testCorrectEquals() {
        BoostedDelegatingFieldBoost first = new BoostedDelegatingFieldBoost(new ConstantFieldBoost(666f), 4f);
        BoostedDelegatingFieldBoost second = new BoostedDelegatingFieldBoost(new ConstantFieldBoost(666f), 4f);
        BoostedDelegatingFieldBoost third = new BoostedDelegatingFieldBoost(new ConstantFieldBoost(666f), 4.000001f);
        BoostedDelegatingFieldBoost fourth = new BoostedDelegatingFieldBoost(new ConstantFieldBoost(667f), 4f);

        assertEquals(first, second);
        assertNotEquals(first, third);
        assertNotEquals(first, fourth);
        assertNotEquals(third, fourth);
    }
    
    @Test
    public void testCorrectHashCode() {
        BoostedDelegatingFieldBoost first = new BoostedDelegatingFieldBoost(new ConstantFieldBoost(666f), 4f);
        BoostedDelegatingFieldBoost second = new BoostedDelegatingFieldBoost(new ConstantFieldBoost(666f), 4f);
        BoostedDelegatingFieldBoost third = new BoostedDelegatingFieldBoost(new ConstantFieldBoost(666f), 4.000001f);
        BoostedDelegatingFieldBoost fourth = new BoostedDelegatingFieldBoost(new ConstantFieldBoost(667f), 4f);

        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first.hashCode(), third.hashCode());
        assertNotEquals(first.hashCode(), fourth.hashCode());
        assertNotEquals(third.hashCode(), fourth.hashCode());
    }

}
