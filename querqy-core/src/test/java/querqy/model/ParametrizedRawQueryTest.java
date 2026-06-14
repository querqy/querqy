/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Querqy Contributors
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

import static org.assertj.core.api.Assertions.assertThat;

public class ParametrizedRawQueryTest {

    @Test
    public void testBuildQueryString() {
        ParametrizedRawQuery rq = new ParametrizedRawQuery(
                null,
                Arrays.asList(
                        new ParametrizedRawQuery.Part("q1 ", ParametrizedRawQuery.Part.Type.QUERY_PART),
                        new ParametrizedRawQuery.Part("p1", ParametrizedRawQuery.Part.Type.PARAMETER),
                        new ParametrizedRawQuery.Part(" q2 ", ParametrizedRawQuery.Part.Type.QUERY_PART),
                        new ParametrizedRawQuery.Part("p2", ParametrizedRawQuery.Part.Type.PARAMETER)
                ),
                Clause.Occur.SHOULD,
                false);

        String queryString = rq.buildQueryString(param -> param.replace("p", "param"));
        assertThat(queryString).isEqualTo("q1 param1 q2 param2");
    }
}
