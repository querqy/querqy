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
import querqy.model.MatchAllQuery;
import querqy.model.convert.AbstractBuilderTest;
import querqy.model.convert.model.Occur;

import static org.assertj.core.api.Assertions.assertThat;
import static querqy.model.convert.builder.MatchAllQueryBuilder.FIELD_NAME_IS_GENERATED;
import static querqy.model.convert.builder.MatchAllQueryBuilder.FIELD_NAME_OCCUR;
import static querqy.model.convert.builder.MatchAllQueryBuilder.matchall;

public class MatchAllQueryBuilderTest extends AbstractBuilderTest {

    @Test
    public void testSetAttributesFromMap() {
        assertThat(new MatchAllQueryBuilder(
                map(
                        entry(MatchAllQueryBuilder.NAME_OF_QUERY_TYPE,
                                map(
                                        entry(FIELD_NAME_OCCUR, "must"),
                                        entry(FIELD_NAME_IS_GENERATED, true)
                                )
                        )
                )


        )).isEqualTo(matchall(Occur.MUST, true));
    }

    @Test
    public void testBuilderToMap() {
        assertThat(matchall(Occur.MUST, true).toMap())
                .isEqualTo(
                        map(
                                entry(MatchAllQueryBuilder.NAME_OF_QUERY_TYPE,
                                        map(
                                                entry(FIELD_NAME_OCCUR, "must"),
                                                entry(FIELD_NAME_IS_GENERATED, true)
                                        )
                                )
                        )
                );
    }

    @Test
    public void testSetAttributesFromObject() {
        MatchAllQueryBuilder matchAllQueryBuilder = matchall(Occur.MUST, true);

        MatchAllQuery matchAllQuery = new MatchAllQuery(null, Clause.Occur.MUST, true);
        assertThat(new MatchAllQueryBuilder(matchAllQuery)).isEqualTo(matchAllQueryBuilder);
    }

    @Test
    public void testBuild() {
        MatchAllQueryBuilder matchAllQueryBuilder = matchall(Occur.MUST, true);

        MatchAllQuery matchAllQuery = new MatchAllQuery(null, Clause.Occur.MUST, true);

        assertThat(matchAllQueryBuilder.build()).isEqualTo(matchAllQuery);
    }
}
