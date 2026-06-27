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
package querqy.rewriter.commonrules.model;

import org.junit.Test;
import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.model.convert.builder.BooleanQueryBuilder;
import querqy.rewriter.commonrules.AbstractCommonRulesTest;
import querqy.rewriter.commonrules.CommonRulesRewriter;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static querqy.model.convert.builder.BooleanQueryBuilder.bq;
import static querqy.model.convert.builder.DisjunctionMaxQueryBuilder.dmq;
import static querqy.model.convert.builder.TermBuilder.term;

public class DeleteInstructionNestedQueriesTest extends AbstractCommonRulesTest {

    @Test
    public void testDeletionOfNestedBooleanQuery() {
        CommonRulesRewriter rewriter = rewriter(
                rule(input("a", "b"),
                        delete("a", "b"))
        );

        BooleanQueryBuilder q =
                bq(
                        dmq(
                                term("ab"),
                                BooleanQueryBuilder.bq(
                                        dmq("a"),
                                        dmq("b")
                                )
                        ),
                        dmq("b")
                );

        ExpandedQuery query = new ExpandedQuery(q.buildQuerqyQuery());
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(new BooleanQueryBuilder(rewritten)).isEqualTo(bq(dmq("ab"), dmq("b")));
    }

    @Test
    public void testThatSequencesAcrossNestedBooleanQueriesAreNotRemoved() {

        CommonRulesRewriter rewriter = rewriter(
            rule(input("a", "b"),
                    delete("a", "b"))
        );

        BooleanQueryBuilder q =
                bq(
                        dmq(
                                term("a"),
                                BooleanQueryBuilder.bq(
                                        dmq("b")
                                )
                        )
                );

        ExpandedQuery query = new ExpandedQuery(q.buildQuerqyQuery());
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(new BooleanQueryBuilder(rewritten)).isEqualTo(q);
    }


    // TODO: move subsequent tests to DeleteInstructionTest as soon as the convert lib is more established

    @Test
    public void testThatDmqContainingTheSameTermMultipleTimesIsFullyRemoved() {
        CommonRulesRewriter rewriter = rewriter(
                rule(input("a"),
                        delete("a"))
        );

        BooleanQueryBuilder q = bq(dmq("a", "a", "a"));

        ExpandedQuery query = new ExpandedQuery(q.buildQuerqyQuery());
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(new BooleanQueryBuilder(rewritten)).isEqualTo(bq(Collections.emptyList()));
    }

    @Test
    public void testThatMultipleDmqsContainingTheSameTermAreFullyRemoved() {
        CommonRulesRewriter rewriter = rewriter(
                rule(input("a"),
                        delete("a"))
        );

        BooleanQueryBuilder q = bq("a", "a", "a");

        ExpandedQuery query = new ExpandedQuery(q.buildQuerqyQuery());
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(new BooleanQueryBuilder(rewritten)).isEqualTo(bq(Collections.emptyList()));
    }


    // TODO: move subsequent tests as soon as the convert lib is more established

    @Test
    public void testThatSynonymsAreAppliedOnDuplicateSequences() {
        CommonRulesRewriter rewriter = rewriter(
                rule(input("a", "b"),
                        synonym("c"))
        );

        BooleanQueryBuilder q = bq("a", "b", "a", "b");

        ExpandedQuery query = new ExpandedQuery(q.buildQuerqyQuery());
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(new BooleanQueryBuilder(rewritten))
                .isEqualTo(
                        bq(
                            dmq(term("a"), term("c", true)),
                            dmq(term("b"), term("c", true)),
                            dmq(term("a"), term("c", true)),
                            dmq(term("b"), term("c", true))));
    }
}
