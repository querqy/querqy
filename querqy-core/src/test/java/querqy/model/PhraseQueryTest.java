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
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static querqy.model.Clause.Occur.MUST;
import static querqy.model.Clause.Occur.SHOULD;

public class PhraseQueryTest {

    private static final List<String> TERMS = Arrays.asList("laptop", "bag");

    @Test
    public void testGetTermsAndSlop() {
        final PhraseQuery pq = new PhraseQuery(null, SHOULD, true, TERMS, 2);
        assertThat(pq.getTerms()).containsExactly("laptop", "bag");
        assertThat(pq.getSlop()).isEqualTo(2);
    }

    @Test
    public void testFieldIsNullByDefault() {
        final PhraseQuery pq = new PhraseQuery(null, SHOULD, true, TERMS, 0);
        assertThat(pq.getField()).isNull();
    }

    @Test
    public void testFieldIsSetWhenProvided() {
        final PhraseQuery pq = new PhraseQuery(null, SHOULD, true, "title", TERMS, 0);
        assertThat(pq.getField()).isEqualTo("title");
    }

    @Test
    public void testNullTermsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new PhraseQuery(null, SHOULD, true, null, 0));
    }

    @Test
    public void testEmptyTermsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new PhraseQuery(null, SHOULD, true, Collections.emptyList(), 0));
    }

    @Test
    public void testNegativeSlopThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new PhraseQuery(null, SHOULD, true, TERMS, -1));
    }

    @Test
    public void testCloneWithBooleanQueryParent() {
        final BooleanQuery boolParent = new BooleanQuery(null, SHOULD, false);
        final PhraseQuery original = new PhraseQuery(null, SHOULD, true, TERMS, 1);

        final BooleanClause cloned = original.clone(boolParent);

        assertThat(cloned).isInstanceOf(PhraseQuery.class);
        final PhraseQuery clonedPq = (PhraseQuery) cloned;
        assertThat(clonedPq.getTerms()).isEqualTo(TERMS);
        assertThat(clonedPq.getSlop()).isEqualTo(1);
        assertThat(clonedPq.isGenerated()).isTrue();
        assertThat(clonedPq.occur).isEqualTo(SHOULD);
    }

    @Test
    public void testClonePreservesField() {
        final BooleanQuery boolParent = new BooleanQuery(null, SHOULD, false);
        final PhraseQuery original = new PhraseQuery(null, SHOULD, true, "title", TERMS, 0);

        assertThat(((PhraseQuery) original.clone(boolParent)).getField()).isEqualTo("title");
        assertThat(((PhraseQuery) original.clone(boolParent, false)).getField()).isEqualTo("title");
        assertThat(((PhraseQuery) original.clone(boolParent, MUST)).getField()).isEqualTo("title");
        assertThat(((PhraseQuery) original.clone(boolParent, MUST, false)).getField()).isEqualTo("title");
    }

    @Test
    public void testClonePreservesFieldForDmq() {
        final DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(null, SHOULD, false);
        final PhraseQuery original = new PhraseQuery(null, SHOULD, true, "title", TERMS, 0);

        assertThat(((PhraseQuery) original.clone(dmq, false)).getField()).isEqualTo("title");
    }

    @Test
    public void testCloneWithOccurOverride() {
        final BooleanQuery boolParent = new BooleanQuery(null, SHOULD, false);
        final PhraseQuery original = new PhraseQuery(null, SHOULD, true, TERMS, 0);

        final BooleanClause cloned = original.clone(boolParent, MUST);

        assertThat(((PhraseQuery) cloned).occur).isEqualTo(MUST);
        assertThat(((PhraseQuery) cloned).isGenerated()).isTrue();
    }

    @Test
    public void testCloneWithGeneratedOverride() {
        final BooleanQuery boolParent = new BooleanQuery(null, SHOULD, false);
        final PhraseQuery original = new PhraseQuery(null, SHOULD, true, TERMS, 0);

        final BooleanClause cloned = original.clone(boolParent, false);

        assertThat(((PhraseQuery) cloned).isGenerated()).isFalse();
        assertThat(((PhraseQuery) cloned).occur).isEqualTo(SHOULD);
    }

    @Test
    public void testCloneWithDmqParent() {
        final DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(null, SHOULD, false);
        final PhraseQuery original = new PhraseQuery(null, SHOULD, true, TERMS, 0);

        final DisjunctionMaxClause cloned = original.clone(dmq, false);

        assertThat(cloned).isInstanceOf(PhraseQuery.class);
        assertThat(((PhraseQuery) cloned).isGenerated()).isFalse();
    }

    @Test
    public void testEquality() {
        final PhraseQuery pq1 = new PhraseQuery(null, SHOULD, true, TERMS, 2);
        final PhraseQuery pq2 = new PhraseQuery(null, SHOULD, true, TERMS, 2);
        assertEquals(pq1, pq2);
        assertEquals(pq1.hashCode(), pq2.hashCode());
    }

    @Test
    public void testEqualityWithSameField() {
        final PhraseQuery pq1 = new PhraseQuery(null, SHOULD, true, "title", TERMS, 0);
        final PhraseQuery pq2 = new PhraseQuery(null, SHOULD, true, "title", TERMS, 0);
        assertEquals(pq1, pq2);
        assertEquals(pq1.hashCode(), pq2.hashCode());
    }

    @Test
    public void testInequalityDifferentField() {
        final PhraseQuery pq1 = new PhraseQuery(null, SHOULD, true, "title", TERMS, 0);
        final PhraseQuery pq2 = new PhraseQuery(null, SHOULD, true, "body", TERMS, 0);
        assertNotEquals(pq1, pq2);
    }

    @Test
    public void testInequalityFieldVsNoField() {
        final PhraseQuery pq1 = new PhraseQuery(null, SHOULD, true, "title", TERMS, 0);
        final PhraseQuery pq2 = new PhraseQuery(null, SHOULD, true, TERMS, 0);
        assertNotEquals(pq1, pq2);
    }

    @Test
    public void testInequalityDifferentSlop() {
        final PhraseQuery pq1 = new PhraseQuery(null, SHOULD, true, TERMS, 1);
        final PhraseQuery pq2 = new PhraseQuery(null, SHOULD, true, TERMS, 2);
        assertNotEquals(pq1, pq2);
    }

    @Test
    public void testInequalityDifferentTerms() {
        final PhraseQuery pq1 = new PhraseQuery(null, SHOULD, true, TERMS, 0);
        final PhraseQuery pq2 = new PhraseQuery(null, SHOULD, true, Collections.singletonList("laptop"), 0);
        assertNotEquals(pq1, pq2);
    }

    @Test
    public void testToStringContainsFieldTermsAndSlop() {
        final PhraseQuery pq = new PhraseQuery(null, SHOULD, true, "title", TERMS, 3);
        assertThat(pq.toString()).contains("title").contains("laptop").contains("bag").contains("3");
    }

    @Test
    public void testAcceptCallsVisitor() {
        final PhraseQuery pq = new PhraseQuery(null, SHOULD, true, TERMS, 0);
        final boolean[] visited = {false};
        pq.accept(new AbstractNodeVisitor<Void>() {
            @Override
            public Void visit(final PhraseQuery phraseQuery) {
                visited[0] = true;
                return null;
            }
        });
        assertThat(visited[0]).isTrue();
    }
}
