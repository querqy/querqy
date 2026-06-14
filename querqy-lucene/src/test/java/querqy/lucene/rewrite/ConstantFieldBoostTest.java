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
package querqy.lucene.rewrite;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by rene on 04/09/2016.
 */
public class ConstantFieldBoostTest {

    @Test
    public void testThatEqualsDependsOnBoostFactor() throws Exception {

        ConstantFieldBoost fieldBoost1 = new ConstantFieldBoost(1f);
        ConstantFieldBoost fieldBoost1a = new ConstantFieldBoost(1f);
        ConstantFieldBoost fieldBoost2 = new ConstantFieldBoost(2f);

        assertEquals(fieldBoost1, fieldBoost1a);

        assertNotEquals(fieldBoost1, fieldBoost2);
    }

    @Test
    public void testThatHashCodeDependsOnBoostFactor() throws Exception {

        ConstantFieldBoost fieldBoost1 = new ConstantFieldBoost(1f);
        ConstantFieldBoost fieldBoost1a = new ConstantFieldBoost(1f);
        ConstantFieldBoost fieldBoost2 = new ConstantFieldBoost(2f);

        assertEquals(fieldBoost1.hashCode(), fieldBoost1a.hashCode());

        assertNotEquals(fieldBoost1.hashCode(), fieldBoost2.hashCode());

    }
}
