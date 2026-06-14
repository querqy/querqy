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
package querqy.rewrite.experimental;

import org.junit.Test;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static querqy.model.convert.builder.BooleanQueryBuilder.bq;
import static querqy.model.convert.builder.DisjunctionMaxQueryBuilder.dmq;
import static querqy.model.convert.builder.ExpandedQueryBuilder.expanded;
import static querqy.model.convert.builder.TermBuilder.term;

public class QueryRewritingHandlerTest {

    @Test
    public void testCommonRulesRewriter() throws IOException {
        assertThat(QueryRewritingHandler.builder()
                .addCommonRulesRewriter("a => \n SYNONYM: b")
                .build()
                .rewriteQuery("a c")
                .getQuery())
                .isEqualTo(
                        expanded(
                                bq(
                                        dmq(
                                                term("a", false),
                                                term("b", true)
                                        ),
                                        dmq("c")
                                )
                        )
                );
    }

    @Test
    public void testRewriteChain() throws IOException {
        assertThat(QueryRewritingHandler.builder()
                .addReplaceRewriter("c => a")
                .addCommonRulesRewriter("a => \n SYNONYM: b")
                .build()
                .rewriteQuery("c")
                .getQuery())
                .isEqualTo(
                        expanded(
                                bq(
                                        dmq(
                                                term("a", false),
                                                term("b", true)
                                        )
                                )
                        )
                );
    }

    @Test
    public void testThatEmptySetIsReturnedIfNoDecorationIsAdded() throws IOException {
        assertThat(QueryRewritingHandler.builder()
                .addCommonRulesRewriter("a => \n SYNONYM: b")
                .build()
                .rewriteQuery("a c").getDecorations()).isEmpty();
    }

    @Test
    public void testThatEmptyMapIsReturnedIfNoNamedDecorationIsAdded() throws IOException {
        assertThat(QueryRewritingHandler.builder()
                .addCommonRulesRewriter("a => \n SYNONYM: b")
                .build()
                .rewriteQuery("a c").getNamedDecorations()).isEmpty();
    }

    @Test
    public void testThatSetContainsDecoration() throws IOException {
        assertThat(QueryRewritingHandler.builder()
                .addCommonRulesRewriter("a => \n DECORATE: b")
                .build()
                .rewriteQuery("a c").getDecorations()).containsExactly("b");
    }

    @Test
    public void testThatMapContainsNamedDecoration() throws IOException {
        assertThat(QueryRewritingHandler.builder()
                .addCommonRulesRewriter("a => \n DECORATE(b): c")
                .build()
                .rewriteQuery("a c").getNamedDecorations())
                .containsExactly(new AbstractMap.SimpleEntry<>("b", Collections.singletonList("c")));
    }



}
