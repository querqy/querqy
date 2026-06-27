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
package querqy.rewriter.commonrules;

import org.junit.Test;
import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.ExpandedQuery;
import querqy.rewriter.commonrules.AbstractCommonRulesTest;
import querqy.rewriter.commonrules.CommonRulesRewriter;
import querqy.rewriter.commonrules.select.booleaninput.model.BooleanInputLiteral;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static querqy.model.convert.builder.BooleanQueryBuilder.bq;

public class CommonRulesRewriterBooleanInputTest extends AbstractCommonRulesTest {

    @Test
    public void testRewritingForSingleBooleanInput() {
        final List<BooleanInputLiteral> literals = literals("a", "b");
        booleanInput(literals, filter("f"));

        CommonRulesRewriter rewriter = rewriter(literals);

        ExpandedQuery expandedQuery = rewriter.rewrite(new ExpandedQuery(bq("a", "b", "c").buildQuerqyQuery()),
                new EmptySearchEngineRequestAdapter()).getExpandedQuery();

        assertThat(expandedQuery.getFilterQueries()).isNotNull();
        assertThat(expandedQuery.getFilterQueries()).hasSize(1);
    }

    @Test
    public void testRewritingForMultipleBooleanInput() {
        final List<BooleanInputLiteral> literals = literals("a", "b", "c", "d");
        booleanInput(literals.subList(0, 2), filter("f"));
        booleanInput(literals.subList(1, 3), filter("g"));
        booleanInput(literals.subList(3, 4), filter("h"));

        CommonRulesRewriter rewriter = rewriter(literals);

        ExpandedQuery expandedQuery;

        expandedQuery= rewriter.rewrite(
                new ExpandedQuery(bq("a", "b").buildQuerqyQuery()), new EmptySearchEngineRequestAdapter()).getExpandedQuery();

        assertThat(expandedQuery.getFilterQueries()).isNotNull();
        assertThat(expandedQuery.getFilterQueries()).hasSize(1);

        expandedQuery= rewriter.rewrite(
                new ExpandedQuery(bq("a", "b", "c").buildQuerqyQuery()), new EmptySearchEngineRequestAdapter()).getExpandedQuery();

        assertThat(expandedQuery.getFilterQueries()).isNotNull();
        assertThat(expandedQuery.getFilterQueries()).hasSize(2);

        expandedQuery= rewriter.rewrite(
                new ExpandedQuery(bq("b", "c").buildQuerqyQuery()), new EmptySearchEngineRequestAdapter()).getExpandedQuery();

        assertThat(expandedQuery.getFilterQueries()).isNotNull();
        assertThat(expandedQuery.getFilterQueries()).hasSize(1);

        expandedQuery= rewriter.rewrite(
                new ExpandedQuery(bq("b", "c", "d").buildQuerqyQuery()), new EmptySearchEngineRequestAdapter()).getExpandedQuery();

        assertThat(expandedQuery.getFilterQueries()).isNotNull();
        assertThat(expandedQuery.getFilterQueries()).hasSize(2);

        expandedQuery= rewriter.rewrite(
                new ExpandedQuery(bq("a", "b", "c", "d").buildQuerqyQuery()), new EmptySearchEngineRequestAdapter()).getExpandedQuery();

        assertThat(expandedQuery.getFilterQueries()).isNotNull();
        assertThat(expandedQuery.getFilterQueries()).hasSize(3);

    }

}
