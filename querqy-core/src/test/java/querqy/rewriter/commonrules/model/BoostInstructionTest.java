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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertFalse;
import static querqy.QuerqyMatchers.boostQ;
import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.must;
import static querqy.QuerqyMatchers.mustNot;
import static querqy.QuerqyMatchers.term;

import java.util.*;

import org.junit.Assert;
import org.junit.Test;

import querqy.model.*;
import querqy.rewriter.commonrules.AbstractCommonRulesTest;
import querqy.rewriter.commonrules.CommonRulesRewriter;
import querqy.rewriter.commonrules.model.BoostInstruction.BoostMethod;

public class BoostInstructionTest extends AbstractCommonRulesTest {
    
    @Test
    public void testThatBoostQueriesAreMarkedAsGenerated() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("x"),
                        boostUp("a", 0.5f)
                )
        );

        ExpandedQuery query = makeQuery("x");
        Collection<BoostQuery> upQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getExpandedQuery().getBoostUpQueries();

        assertThat(upQueries,
              contains( 
                      boostQ(
                              bq(
                                      dmq(must(), term("a", true))
                              ),
                              0.5f
                              
              )));
    }

    @Test
    public void testThatBoostQueriesUseMM100ByDefault() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("x"),
                        boostUp("a b", 0.5f)
                )
        );

        ExpandedQuery query = makeQuery("x");
        Collection<BoostQuery> upQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getExpandedQuery().getBoostUpQueries();

        assertThat(upQueries,
                contains(
                        boostQ(
                                bq(
                                        dmq(must(), term("a", true)),
                                        dmq(must(), term("b", true))
                                ),
                                0.5f

                        )));
    }

    @Test
    public void testThatBoostQueriesWithMustClauseUseMM100ByDefault() {

        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("x"),
                        boostUp("a b", 0.5f)
                )
        );

        ExpandedQuery query = makeQuery("x");
        Collection<BoostQuery> upQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getExpandedQuery().getBoostUpQueries();

        assertThat(upQueries,
                contains(
                        boostQ(
                                bq(
                                        dmq(must(), term("a", true)),
                                        dmq(must(), term("b", true))
                                ),
                                0.5f

                        )));



    }

    @Test
    public void testThatBoostQueriesWithMustNotClauseUseMM100ByDefault() {

        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("x"),
                        boostUp("-a b", 0.5f)
                )
        );

        ExpandedQuery query = makeQuery("x");
        Collection<BoostQuery> upQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getExpandedQuery().getBoostUpQueries();

        assertThat(upQueries,
                contains(
                        boostQ(
                                bq(
                                        dmq(mustNot(), term("a", true)),
                                        dmq(must(), term("b", true))
                                ),
                                0.5f

                        )));



    }

    @Test
    public void testPurelyNegativeBoostQuery() {

        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("x"),
                        boostUp( "-a", 0.5f)
                )
        );

        ExpandedQuery query = makeQuery("x");
        Collection<BoostQuery> upQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getExpandedQuery().getBoostUpQueries();

        assertThat(upQueries,
                contains(
                        boostQ(
                                bq(
                                        dmq(mustNot(), term("a", true))
                                ),
                                0.5f

                        )));


    }

    @Test
    public void testMultiplicativeBoostQuery() {

        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("x"),
                        boostUp("a", 2f, BoostMethod.MULTIPLICATIVE)
                )
        );

        ExpandedQuery query = makeQuery("x");
        Collection<BoostQuery> multiplicativeBoostQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getExpandedQuery().getMultiplicativeBoostQueries();

        assertThat(multiplicativeBoostQueries,
                contains(
                        boostQ(
                                bq(
                                        dmq(must(), term("a", true))
                                ),
                                2f

                        )));
    }

    @Test
    public void testMultiplicativeDownBoost() {

        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("x"),
                        boostUp("a", 0.5f, BoostMethod.MULTIPLICATIVE)
                )
        );

        ExpandedQuery query = makeQuery("x");
        Collection<BoostQuery> multiplicativeBoostQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getExpandedQuery().getMultiplicativeBoostQueries();

        assertThat(multiplicativeBoostQueries,
                contains(
                        boostQ(
                                bq(
                                        dmq(must(), term("a", true))
                                ),
                                0.5f

                        )));
    }


    @Test
    public void testThatMainQueryIsNotMarkedAsGenerated() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("x"),
                        boostUp("a", 0.5f)
                )
        );

        ExpandedQuery query = makeQuery("x");
        QuerqyQuery<?> mainQuery = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertFalse(mainQuery.isGenerated());

    }

    @Test
    public void testThatUpQueriesAreOfTypeQuery() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("x"),
                        boostUp("a b", 0.5f)
                )
        );

        ExpandedQuery query = makeQuery("x");
        Collection<BoostQuery> upQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getExpandedQuery().getBoostUpQueries();
        for (BoostQuery bq : upQueries) {
            Assert.assertTrue(bq.getQuery() instanceof Query);
        }

    }

    @Test
    public void testThatDownQueriesAreOfTypeQuery() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("x"),
                        boostDown("a", 0.5f)
                ),
                rule(
                        input("y"),
                        boostDown("a", 0.5f)
                )
        );

        ExpandedQuery query = makeQuery("x y");

        Collection<BoostQuery> downQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getExpandedQuery().getBoostDownQueries();


        for (BoostQuery bq : downQueries) {
            Assert.assertTrue(bq.getQuery() instanceof Query);
        }

    }
}
