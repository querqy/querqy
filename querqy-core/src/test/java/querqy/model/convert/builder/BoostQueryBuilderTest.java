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
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import querqy.model.BoostQuery;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.model.convert.AbstractBuilderTest;
import querqy.model.convert.model.QuerqyQueryBuilder;
import querqy.model.convert.QueryBuilderException;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static querqy.model.convert.builder.BooleanQueryBuilder.bq;
import static querqy.model.convert.builder.BoostQueryBuilder.FIELD_NAME_BOOST;
import static querqy.model.convert.builder.BoostQueryBuilder.FIELD_NAME_QUERY;
import static querqy.model.convert.builder.BoostQueryBuilder.boost;

@RunWith(MockitoJUnitRunner.class)
public class BoostQueryBuilderTest extends AbstractBuilderTest {

    @Test
    public void testThatExceptionIsThrownQueryValueIsNull() {
        assertThatThrownBy(() -> new BoostQueryBuilder(Collections.emptyMap()).build())
                .isInstanceOf(QueryBuilderException.class);
    }

    @Test
    public void testThatNoExceptionIsThrownIfQueryIsNotNull() {
        assertThatCode(() -> new BoostQueryBuilder(mock(QuerqyQueryBuilder.class)).build()).doesNotThrowAnyException();
    }

    @Test
    public void testSetAttributesFromMap() {
        assertThat(new BoostQueryBuilder(
                map(
                        entry(BoostQueryBuilder.NAME_OF_QUERY_TYPE,
                                map(
                                        entry(FIELD_NAME_QUERY, bq("a").toMap()),
                                        entry(FIELD_NAME_BOOST, 1.0f)))

                ))).isEqualTo(boost(bq("a"), 1.0f));
    }

    @Test
    public void testBuilderToMap() {
        BoostQueryBuilder boostBuilder = boost(bq("a"), 1.0f);

        assertThat(boostBuilder.toMap())
                .isEqualTo(
                        map(
                                entry(
                                        BoostQueryBuilder.NAME_OF_QUERY_TYPE,
                                        map(
                                                entry(FIELD_NAME_QUERY, bq("a").toMap()),
                                                entry(FIELD_NAME_BOOST, 1.0f))
                                )
                        )
                );
    }

    @Test
    public void testSetAttributesFromObject() {
        BoostQueryBuilder boostBuilder = boost(bq("a"), 1.0f);

        Query query = new Query();

        DisjunctionMaxQuery dmq1 = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, false);
        Term term1 = new Term(dmq1, "a");
        dmq1.addClause(term1);
        query.addClause(dmq1);

        assertThat(new BoostQueryBuilder(new BoostQuery(query, 1.0f))).isEqualTo(boostBuilder);
    }

    @Test
    public void testBuild() {
        BoostQueryBuilder boostBuilder = boost(bq("a"), 1.0f);

        Query query = new Query();

        DisjunctionMaxQuery dmq1 = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, false);
        Term term1 = new Term(dmq1, "a");
        dmq1.addClause(term1);
        query.addClause(dmq1);

        BoostQuery expectedBoostQuery = new BoostQuery(query, 1.0f);
        BoostQuery actualBoostQuery = boostBuilder.build();

        assertThat(actualBoostQuery).isEqualTo(expectedBoostQuery);
    }

}
