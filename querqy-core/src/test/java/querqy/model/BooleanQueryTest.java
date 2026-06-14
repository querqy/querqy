/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2017 Querqy Contributors
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
package querqy.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static querqy.model.Clause.Occur.MUST;
import static querqy.model.Clause.Occur.MUST_NOT;
import static querqy.model.Clause.Occur.SHOULD;
import static querqy.QuerqyMatchers.*;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by rene on 08/07/2017.
 */
public class BooleanQueryTest {

    private Object TypeSafeMatcher;

    @Test
    public void testThatClonePreservesGeneratedAndOccur() throws Exception {
        BooleanQuery bq = new BooleanQuery(null, MUST_NOT, true);
        final BooleanClause clone = bq.clone(null);
        assertEquals(MUST_NOT, clone.getOccur());
        assertTrue(clone.isGenerated());
    }

    @Test
    public void testThatGeneratedIsPropagatedToClauses() throws Exception {

        BooleanQuery bq = new BooleanQuery(null, SHOULD, false);
        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(bq, Clause.Occur.SHOULD, false);
        bq.addClause(dmq);
        dmq.addClause(new Term(dmq, "Test", false));

        final BooleanQuery clone = (BooleanQuery) bq.clone(null, MUST, true);

        assertThat(clone, bq(must(), dmq(should(), term("Test", true))));
        assertTrue(clone.isGenerated());
        assertTrue(clone.getClauses().get(0).isGenerated());
        assertEquals(MUST, clone.getOccur());

    }
}
