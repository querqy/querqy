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
package querqy.model.convert.builder;

import org.junit.Test;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Term;
import querqy.model.convert.AbstractBuilderTest;
import querqy.model.convert.model.Occur;

import static org.assertj.core.api.Assertions.assertThat;
import static querqy.model.convert.builder.DisjunctionMaxQueryBuilder.FIELD_NAME_CLAUSES;
import static querqy.model.convert.builder.DisjunctionMaxQueryBuilder.FIELD_NAME_IS_GENERATED;
import static querqy.model.convert.builder.DisjunctionMaxQueryBuilder.FIELD_NAME_OCCUR;
import static querqy.model.convert.builder.DisjunctionMaxQueryBuilder.dmq;
import static querqy.model.convert.builder.TermBuilder.term;

public class DisjunctionMaxQueryBuilderTest extends AbstractBuilderTest {

    @Test
    public void testSetAttributesFromMap() {
        assertThat(new DisjunctionMaxQueryBuilder(
                map(
                        entry(DisjunctionMaxQueryBuilder.NAME_OF_QUERY_TYPE,
                                map(
                                        entry(FIELD_NAME_CLAUSES,
                                                list(
                                                        term("a").toMap(),
                                                        term("b").toMap())),
                                        entry(FIELD_NAME_OCCUR, Occur.MUST.typeName),
                                        entry(FIELD_NAME_IS_GENERATED, "true")))

                ))).isEqualTo(dmq(list(term("a"), term("b")), Occur.MUST, true));
    }

    @Test
    public void testBuilderToMap() {
        DisjunctionMaxQueryBuilder dmqBuilder = dmq(list(term("a"), term("b")), Occur.MUST, true);

        assertThat(dmqBuilder.toMap())
                .isEqualTo(
                        map(
                                entry(DisjunctionMaxQueryBuilder.NAME_OF_QUERY_TYPE,
                                        map(
                                                entry(FIELD_NAME_CLAUSES,
                                                        list(
                                                                term("a").toMap(),
                                                                term("b").toMap())
                                                ),
                                                entry(FIELD_NAME_OCCUR, Occur.MUST.typeName),
                                                entry(FIELD_NAME_IS_GENERATED, true)))
                        )
                );
    }

    @Test
    public void testSetAttributesFromObject() {
        DisjunctionMaxQueryBuilder dmqBuilder = dmq(list(term("a"), term("b")), Occur.MUST, true);

        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(null, Clause.Occur.MUST, true);
        Term term1 = new Term(dmq, "a");
        Term term2 = new Term(dmq, "b");
        dmq.addClause(term1);
        dmq.addClause(term2);

        assertThat(new DisjunctionMaxQueryBuilder(dmq)).isEqualTo(dmqBuilder);
    }

    @Test
    public void testBuild() {
        DisjunctionMaxQueryBuilder dmqBuilder = dmq(list(term("a"), term("b")), Occur.MUST, true);

        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(null, Clause.Occur.MUST, true);
        Term term1 = new Term(dmq, "a");
        Term term2 = new Term(dmq, "b");
        dmq.addClause(term1);
        dmq.addClause(term2);

        assertThat(dmqBuilder.build()).isEqualTo(dmq);
    }
}
