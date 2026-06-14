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
import querqy.model.StringRawQuery;
import querqy.model.convert.AbstractBuilderTest;
import querqy.model.convert.QueryBuilderException;
import querqy.model.convert.model.Occur;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static querqy.model.convert.builder.StringRawQueryBuilder.FIELD_NAME_IS_GENERATED;
import static querqy.model.convert.builder.StringRawQueryBuilder.FIELD_NAME_OCCUR;
import static querqy.model.convert.builder.StringRawQueryBuilder.FIELD_NAME_RAW_QUERY;
import static querqy.model.convert.builder.StringRawQueryBuilder.raw;

public class StringRawQueryBuilderTest extends AbstractBuilderTest {

    @Test
    public void testThatExceptionIsThrownIfRawQueryIsNull() {
        assertThatThrownBy(() -> raw(null).build())
                .isInstanceOf(QueryBuilderException.class);
    }

    @Test
    public void testThatNoExceptionIsThrownIfRawQueryIsNotNull() {
        assertThatCode(() -> raw("raw").build()).doesNotThrowAnyException();
    }

    @Test
    public void testSetAttributesFromMap() {

        assertThat(new StringRawQueryBuilder(
                map(
                        entry(StringRawQueryBuilder.NAME_OF_QUERY_TYPE, map(
                                entry(FIELD_NAME_RAW_QUERY, "a"),
                                entry(FIELD_NAME_OCCUR, "must"),
                                entry(FIELD_NAME_IS_GENERATED, true)))
                )
        )).isEqualTo(new StringRawQueryBuilder("a", Occur.MUST, true));

    }

    @Test
    public void testBuilderToMap() {
        assertThat(new StringRawQueryBuilder("a", Occur.MUST, true).toMap())
                .isEqualTo(
                        map(
                                entry(StringRawQueryBuilder.NAME_OF_QUERY_TYPE,
                                        map(
                                                entry(FIELD_NAME_RAW_QUERY, "a"),
                                                entry(FIELD_NAME_OCCUR, "must"),
                                                entry(FIELD_NAME_IS_GENERATED, true)))
                        )
                );
    }

    @Test
    public void testSetAttributesFromObject() {
        StringRawQueryBuilder stringRawQueryBuilder = new StringRawQueryBuilder("a", Occur.MUST, true);
        StringRawQuery stringRawQuery = new StringRawQuery(null,"a", Clause.Occur.MUST, true);
        assertThat(new StringRawQueryBuilder(stringRawQuery)).isEqualTo(stringRawQueryBuilder);
    }

    @Test
    public void testBuild() {
        StringRawQueryBuilder stringRawQueryBuilder = new StringRawQueryBuilder("a", Occur.MUST, true);
        StringRawQuery stringRawQuery = new StringRawQuery(null,"a", Clause.Occur.MUST, true);
        assertThat(stringRawQueryBuilder.build()).isEqualTo(stringRawQuery);
    }
}
