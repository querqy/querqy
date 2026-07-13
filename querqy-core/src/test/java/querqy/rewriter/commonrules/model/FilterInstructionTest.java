/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2015 Querqy Contributors
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

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import querqy.model.*;
import querqy.rewriter.commonrules.AbstractCommonRulesTest;
import querqy.rewriter.commonrules.CommonRulesRewriter;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static querqy.QuerqyMatchers.*;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.term;

/**
 * Created by rene on 08/12/2015.
 */
public class FilterInstructionTest  extends AbstractCommonRulesTest {

    @Test
    public void testThatBoostQueriesWithMustClauseUseMM100ByDefault() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("x"),
                        filter("a b")
                )
        );

        ExpandedQuery query = makeQuery("x");
        Collection<QuerqyQuery<?>> filterQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getExpandedQuery().getFilterQueries();

        QuerqyQuery<?> qq = filterQueries.iterator().next();
        assertTrue(qq instanceof BooleanQuery);


        assertThat((BooleanQuery) qq,
                bq(
                    dmq(must(), term("a", true)),
                    dmq(must(), term("b", true))
                )
        );

    }


    @Test
    public void testPurelyNegativeFilterQuery() {

        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("x"),
                        filter("-ab")
                )
        );

        ExpandedQuery query = makeQuery("x");

        Collection<QuerqyQuery<?>> filterQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getExpandedQuery().getFilterQueries();

        assertNotNull(filterQueries);
        assertEquals(1, filterQueries.size());

        QuerqyQuery<?> qq = filterQueries.iterator().next();
        assertTrue(qq instanceof BooleanQuery);

        assertThat((BooleanQuery) qq,
                bq(
                    should(),
                    dmq(
                            mustNot(),
                            term("ab", true)
                    )
                )
        );

    }

    @Test
    public void testThatFilterQueriesAreMarkedAsGenerated() {

        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("x"),
                        filter("a")
                )
        );

        ExpandedQuery query = makeQuery("x");
        Collection<QuerqyQuery<?>> filterQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getExpandedQuery().getFilterQueries();

        QuerqyQuery<?> qq = filterQueries.iterator().next();
        assertTrue(qq instanceof BooleanQuery);


        assertThat((BooleanQuery) qq,
                bq(
                        dmq(must(), term("a", true))
                )
        );

    }

    @Test
    public void testThatMainQueryIsNotMarkedAsGenerated() {

        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("x"),
                        filter("a")
                )
        );

        ExpandedQuery query = makeQuery("x");
        QuerqyQuery<?> mainQuery = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();
        assertFalse(mainQuery.isGenerated());

    }

}
