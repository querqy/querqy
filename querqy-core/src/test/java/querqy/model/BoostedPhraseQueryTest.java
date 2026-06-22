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
package querqy.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static querqy.model.Clause.Occur.MUST;
import static querqy.model.Clause.Occur.SHOULD;

public class BoostedPhraseQueryTest {

    private static final List<String> TERMS = Arrays.asList("laptop", "bag");

    @Test
    public void testGetBoost() {
        final BoostedPhraseQuery bpq = new BoostedPhraseQuery(null, SHOULD, "title", TERMS, 0, 1.5f);
        assertThat(bpq.getBoost()).isEqualTo(1.5f);
    }

    @Test
    public void testGetField() {
        final BoostedPhraseQuery bpq = new BoostedPhraseQuery(null, SHOULD, "title", TERMS, 0, 1.5f);
        assertThat(bpq.getField()).isEqualTo("title");
    }

    @Test
    public void testFieldMayBeNull() {
        final BoostedPhraseQuery bpq = new BoostedPhraseQuery(null, SHOULD, null, TERMS, 0, 2f);
        assertThat(bpq.getField()).isNull();
        assertThat(bpq.getBoost()).isEqualTo(2f);
    }

    @Test
    public void testIsAlwaysGenerated() {
        final BoostedPhraseQuery bpq = new BoostedPhraseQuery(null, SHOULD, null, TERMS, 0, 1f);
        assertThat(bpq.isGenerated()).isTrue();
    }

    @Test
    public void testIsInstanceOfPhraseQuery() {
        assertThat(new BoostedPhraseQuery(null, SHOULD, null, TERMS, 0, 1f))
                .isInstanceOf(PhraseQuery.class);
    }

    // --- accept dispatches to visit(PhraseQuery) ---

    @Test
    public void testAcceptDispatchesToVisitPhraseQuery() {
        final BoostedPhraseQuery bpq = new BoostedPhraseQuery(null, SHOULD, null, TERMS, 0, 1f);
        final boolean[] visitedAsPhrase = {false};
        bpq.accept(new AbstractNodeVisitor<Void>() {
            @Override
            public Void visit(final PhraseQuery phraseQuery) {
                visitedAsPhrase[0] = true;
                assertThat(phraseQuery).isSameAs(bpq);
                return null;
            }
        });
        assertThat(visitedAsPhrase[0]).isTrue();
    }

    // --- clone ---

    @Test
    public void testCloneWithBooleanQueryParentReturnsBoostedPhraseQuery() {
        final BooleanQuery boolParent = new BooleanQuery(null, SHOULD, false);
        final BoostedPhraseQuery original = new BoostedPhraseQuery(null, SHOULD, "title", TERMS, 2, 1.5f);

        final BooleanClause cloned = original.clone(boolParent);

        assertThat(cloned).isInstanceOf(BoostedPhraseQuery.class);
        final BoostedPhraseQuery bpq = (BoostedPhraseQuery) cloned;
        assertThat(bpq.getField()).isEqualTo("title");
        assertThat(bpq.getTerms()).isEqualTo(TERMS);
        assertThat(bpq.getSlop()).isEqualTo(2);
        assertThat(bpq.getBoost()).isEqualTo(1.5f);
        assertThat(bpq.occur).isEqualTo(SHOULD);
    }

    @Test
    public void testCloneWithOccurOverridePreservesBoost() {
        final BooleanQuery boolParent = new BooleanQuery(null, SHOULD, false);
        final BoostedPhraseQuery original = new BoostedPhraseQuery(null, SHOULD, null, TERMS, 0, 2f);

        final BoostedPhraseQuery cloned = (BoostedPhraseQuery) original.clone(boolParent, MUST);

        assertThat(cloned.occur).isEqualTo(MUST);
        assertThat(cloned.getBoost()).isEqualTo(2f);
    }

    @Test
    public void testCloneWithDmqParentReturnsBoostedPhraseQuery() {
        final DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(null, SHOULD, false);
        final BoostedPhraseQuery original = new BoostedPhraseQuery(null, SHOULD, "body", TERMS, 1, 3f);

        final DisjunctionMaxClause cloned = original.clone(dmq, false);

        assertThat(cloned).isInstanceOf(BoostedPhraseQuery.class);
        assertThat(((BoostedPhraseQuery) cloned).getBoost()).isEqualTo(3f);
        assertThat(((BoostedPhraseQuery) cloned).getField()).isEqualTo("body");
    }

    // --- equals / hashCode ---

    @Test
    public void testEqualityWithSameFieldAndBoost() {
        final BoostedPhraseQuery bpq1 = new BoostedPhraseQuery(null, SHOULD, "title", TERMS, 0, 1.5f);
        final BoostedPhraseQuery bpq2 = new BoostedPhraseQuery(null, SHOULD, "title", TERMS, 0, 1.5f);
        assertEquals(bpq1, bpq2);
        assertEquals(bpq1.hashCode(), bpq2.hashCode());
    }

    @Test
    public void testInequalityDifferentBoost() {
        final BoostedPhraseQuery bpq1 = new BoostedPhraseQuery(null, SHOULD, null, TERMS, 0, 1f);
        final BoostedPhraseQuery bpq2 = new BoostedPhraseQuery(null, SHOULD, null, TERMS, 0, 2f);
        assertNotEquals(bpq1, bpq2);
    }

    @Test
    public void testInequalityDifferentField() {
        final BoostedPhraseQuery bpq1 = new BoostedPhraseQuery(null, SHOULD, "title", TERMS, 0, 1f);
        final BoostedPhraseQuery bpq2 = new BoostedPhraseQuery(null, SHOULD, "body", TERMS, 0, 1f);
        assertNotEquals(bpq1, bpq2);
    }

    @Test
    public void testInequalityAgainstPlainPhraseQuery() {
        final BoostedPhraseQuery bpq = new BoostedPhraseQuery(null, SHOULD, null, TERMS, 0, 1f);
        final PhraseQuery pq = new PhraseQuery(null, SHOULD, true, TERMS, 0);
        assertNotEquals(bpq, pq);
        assertNotEquals(pq, bpq);
    }

    @Test
    public void testToStringContainsBoostAndField() {
        final BoostedPhraseQuery bpq = new BoostedPhraseQuery(null, SHOULD, "title", TERMS, 0, 1.5f);
        assertThat(bpq.toString()).contains("title").contains("1.5").contains("laptop").contains("bag");
    }
}
