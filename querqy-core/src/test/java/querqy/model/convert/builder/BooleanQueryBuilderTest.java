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
import querqy.model.BooleanQuery;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Term;
import querqy.model.convert.AbstractBuilderTest;
import querqy.model.convert.model.Occur;

import static org.assertj.core.api.Assertions.assertThat;
import static querqy.model.convert.builder.BooleanQueryBuilder.FIELD_NAME_CLAUSES;
import static querqy.model.convert.builder.BooleanQueryBuilder.FIELD_NAME_IS_GENERATED;
import static querqy.model.convert.builder.BooleanQueryBuilder.FIELD_NAME_OCCUR;
import static querqy.model.convert.builder.BooleanQueryBuilder.bq;
import static querqy.model.convert.builder.DisjunctionMaxQueryBuilder.dmq;

public class BooleanQueryBuilderTest extends AbstractBuilderTest {

    @Test
    public void testSetAttributesFromMap() {
        assertThat(new BooleanQueryBuilder(
                map(
                        entry(BooleanQueryBuilder.NAME_OF_QUERY_TYPE,
                                map(
                                        entry(FIELD_NAME_CLAUSES,
                                                list(
                                                        dmq("a").toMap(),
                                                        dmq("b").toMap())),
                                        entry(FIELD_NAME_OCCUR, Occur.MUST.typeName),
                                        entry(FIELD_NAME_IS_GENERATED, "true")))

                ))).isEqualTo(
                        bq(list(dmq("a"), dmq("b")), Occur.MUST, true));
    }

    @Test
    public void testBuilderToMap() {
        BooleanQueryBuilder bqBuilder = bq(list(dmq("a"), dmq("b")), Occur.MUST, true);

        assertThat(bqBuilder.toMap())
                .isEqualTo(
                        map(
                                entry(BooleanQueryBuilder.NAME_OF_QUERY_TYPE,
                                        map(
                                                entry(FIELD_NAME_CLAUSES,
                                                        list(
                                                                dmq("a").toMap(),
                                                                dmq("b").toMap())),
                                                entry(FIELD_NAME_OCCUR, Occur.MUST.typeName),
                                                entry(FIELD_NAME_IS_GENERATED, true)))));
    }

    @Test
    public void testSetAttributesFromObject() {
        BooleanQueryBuilder bqBuilder = bq(list(dmq("a"), dmq("b")), Occur.MUST, true);

        BooleanQuery bq = new BooleanQuery(null, Clause.Occur.MUST, true);

        DisjunctionMaxQuery dmq1 = new DisjunctionMaxQuery(bq, Clause.Occur.SHOULD, false);
        Term term1 = new Term(dmq1, "a");
        dmq1.addClause(term1);
        bq.addClause(dmq1);

        DisjunctionMaxQuery dmq2 = new DisjunctionMaxQuery(bq, Clause.Occur.SHOULD, false);
        Term term2 = new Term(dmq2, "b");
        dmq2.addClause(term2);
        bq.addClause(dmq2);

        assertThat(new BooleanQueryBuilder(bq)).isEqualTo(bqBuilder);
    }

    @Test
    public void testBuild() {
        BooleanQueryBuilder bqBuilder = bq(list(dmq("a"), dmq("b")), Occur.MUST, true);

        BooleanQuery bq = new BooleanQuery(null, Clause.Occur.MUST, true);

        DisjunctionMaxQuery dmq1 = new DisjunctionMaxQuery(bq, Clause.Occur.SHOULD, false);
        Term term1 = new Term(dmq1, "a");
        dmq1.addClause(term1);
        bq.addClause(dmq1);

        DisjunctionMaxQuery dmq2 = new DisjunctionMaxQuery(bq, Clause.Occur.SHOULD, false);
        Term term2 = new Term(dmq2, "b");
        dmq2.addClause(term2);
        bq.addClause(dmq2);

        assertThat(bqBuilder.build()).isEqualTo(bq);
    }

}
