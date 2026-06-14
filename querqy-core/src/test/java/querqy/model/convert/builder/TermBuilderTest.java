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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Test;
import querqy.model.Term;
import querqy.model.convert.AbstractBuilderTest;
import querqy.model.convert.QueryBuilderException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static querqy.model.convert.builder.TermBuilder.FIELD_NAME_IS_GENERATED;
import static querqy.model.convert.builder.TermBuilder.FIELD_NAME_SEARCH_FIELD;
import static querqy.model.convert.builder.TermBuilder.FIELD_NAME_VALUE;
import static querqy.model.convert.builder.TermBuilder.term;

public class TermBuilderTest extends AbstractBuilderTest {

    @Test
    public void testThatExceptionIsThrownIfValueIsNull() {
        assertThatThrownBy(() -> new TermBuilder(null, null, false).build())
                .isInstanceOf(QueryBuilderException.class);
    }

    @Test
    public void testThatNoExceptionIsThrownIfValueIsNotNull() {
        assertThatCode(() -> new TermBuilder("term", null, false).build()).doesNotThrowAnyException();
    }

    @Test
    public void testSetAttributesFromMap() {
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        assertThat(new TermBuilder(
                map(
                        entry(TermBuilder.NAME_OF_QUERY_TYPE, map(
                                entry(FIELD_NAME_VALUE, "value"),
                                entry(FIELD_NAME_SEARCH_FIELD, "field"),
                                entry(FIELD_NAME_IS_GENERATED, true)))
                )
        )).isEqualTo(term("value", "field", true));

    }

    @Test
    public void testBuilderToMap() {
        assertThat(term("value", "field", true).toMap())
                .isEqualTo(
                        map(
                                entry(TermBuilder.NAME_OF_QUERY_TYPE,
                                        map(
                                                entry(FIELD_NAME_VALUE, "value"),
                                                entry(FIELD_NAME_SEARCH_FIELD, "field"),
                                                entry(FIELD_NAME_IS_GENERATED, true)))
                        )
                );
    }

    @Test
    public void testSetAttributesFromObject() {
        TermBuilder termBuilder = term("a");
        Term term = new Term(null, "a");
        assertThat(new TermBuilder(term)).isEqualTo(termBuilder);
    }

    @Test
    public void testBuild() {
        TermBuilder termBuilder = term("a");
        Term term = new Term(null, "a");
        assertThat(termBuilder.build()).isEqualTo(term);
    }
}
