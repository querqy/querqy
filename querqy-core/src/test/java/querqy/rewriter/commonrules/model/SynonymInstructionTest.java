/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014 Querqy Contributors
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
import static querqy.QuerqyMatchers.*;

import org.junit.Test;

import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.rewriter.commonrules.AbstractCommonRulesTest;
import querqy.rewriter.commonrules.CommonRulesRewriter;

public class SynonymInstructionTest extends AbstractCommonRulesTest {
    
    @Test
    public void testThatSingleTermIsExpandedWithSingleTerm() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("a"),
                        synonym("s1")
                )
        );

        ExpandedQuery query = makeQuery("a");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("a", false),
                              term("s1", true)
                              
                         )
              ));

    }
    
    @Test
    public void testThatTermInManyIsExpandedWithSingleTerm() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("a"),
                        synonym("s1")
                ),
                rule(
                        input("b"),
                        synonym("s2")
                ),
                rule(
                        input("c"),
                        synonym("s3")
                )
        );

        ExpandedQuery query = makeQuery("a b c");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("a", false),
                              term("s1", true)
                              
                         ),
                      dmq(
                              term("b", false),
                              term("s2", true)
                                 
                         ),
                      dmq(
                              term("c", false),
                              term("s3", true)
                                    
                        )
                         
              ));

    }
    
    @Test
    public void testThatSingleTermIsExpandedByMany() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("a"),
                        synonym("s1_1", "s1_2")
                )
        );


        ExpandedQuery query = makeQuery("a");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("a", false),
                              bq(
                                      dmq(must(), term("s1_1", true)),
                                      dmq(must(), term("s1_2", true))
                              )
                              
                         )
              ));
    }
    
    @Test
    public void testThatMultipleTermsAreExpandedBySingle() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("a b"),
                        synonym("s1")
                )
        );

        ExpandedQuery query = makeQuery("a b");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("a", false),
                              term("s1", true)
                              
                         ),
                         dmq(
                                 term("b", false),
                                 term("s1", true)
                            )
                         
                         
              ));
    }
    
    @Test
    public void testThatMultipleTermsAreExpandedByMany() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("a b"),
                        synonym("s1_1", "s1_2")
                )
        );

        ExpandedQuery query = makeQuery("a b c");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("a", false),
                              bq(
                                      dmq(must(), term("s1_1", true)),
                                      dmq(must(), term("s1_2", true))
                              )
                              
                         ),
                         dmq(
                                 term("b", false),
                                 bq(
                                         dmq(must(), term("s1_1", true)),
                                         dmq(must(), term("s1_2", true))
                                        
                                 )
                                 
                            ),
                         dmq(term("c", false))
                         
                         
              ));
    }
    
    @Test
    public void testThatPrefixIsMatchedAndPlaceHolderGetsReplaced() {

        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("p1*"),
                        synonym("p1", "$1")
                )
        );

        ExpandedQuery query = makeQuery("p1xyz");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("p1xyz", false),
                              bq(
                                      dmq(must(), term("p1", true)),
                                      dmq(must(), term("xyz", true))
                              )
                         )
              ));
    }
    
    @Test
    public void testThatPrefixIsMatchedAndPlaceHolderGetsReplacedForLongerTerms() {

        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("bus*"),
                        synonym("bus", "$1")
                )
        );

        ExpandedQuery query = makeQuery("busstop");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("busstop", false),
                              bq(
                                      dmq(must(), term("bus", true)),
                                      dmq(must(), term("stop", true))
                                      
                              )
                              
                         )
                         
                         
              ));
        
    }
    
    @Test
    public void testThatPrefixIsMatchedAndPlaceHolderGetsReplacedAtLastOfTwoTerms() {

        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("p1 p2*"),
                        synonym("p2", "$1")
                )
        );

        ExpandedQuery query = makeQuery("p1 p2xyz");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("p1", false),
                              bq( 
                                      dmq(must(), term("p2", true)),
                                      dmq(must(), term("xyz", true))
                                      )
                         ),
                      dmq(
                              term("p2xyz", false),
                              bq(
                                      dmq(must(), term("p2", true)),
                                      dmq(must(), term("xyz", true))
                                      
                              )
                              
                         )
                         
                         
              ));
        
        
        
    }
    
    @Test
    public void testThatWildcardDoesNotMatchZeroChars() {

        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("p1*"),
                        synonym("p1", "$1")
                )
        );

        ExpandedQuery query = makeQuery("p1");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("p1", false)
                         )
              ));
    }

    @Test
    public void testThatLeadingWildcardIsMatchedAndPlaceHolderGetsReplaced() {

        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("*p1"),
                        synonym("$1", "p1")
                )
        );

        ExpandedQuery query = makeQuery("xyzp1");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("xyzp1", false),
                              bq(
                                      dmq(must(), term("xyz", true)),
                                      dmq(must(), term("p1", true))
                              )
                         )
              ));
    }

    @Test
    public void testThatLeadingWildcardDoesNotMatchZeroChars() {

        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("*p1"),
                        synonym("$1", "p1")
                )
        );

        ExpandedQuery query = makeQuery("p1");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("p1", false)
                         )
              ));
    }

    @Test
    public void testThatLeadingWildcardIsMatchedAsLastOfTwoTerms() {

        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("p1 *p2"),
                        synonym("$1", "p2")
                )
        );

        ExpandedQuery query = makeQuery("p1 xyzp2");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("p1", false),
                              bq(
                                      dmq(must(), term("xyz", true)),
                                      dmq(must(), term("p2", true))
                              )
                         ),
                      dmq(
                              term("xyzp2", false),
                              bq(
                                      dmq(must(), term("xyz", true)),
                                      dmq(must(), term("p2", true))
                              )
                         )
              ));
    }

    @Test
    public void testThatLeadingWildcardIsMatchedAsFirstOfTwoTerms() {

        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("*p1 p2"),
                        synonym("$1", "p1")
                )
        );

        ExpandedQuery query = makeQuery("xyzp1 p2");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("xyzp1", false),
                              bq(
                                      dmq(must(), term("xyz", true)),
                                      dmq(must(), term("p1", true))
                              )
                         ),
                      dmq(
                              term("p2", false),
                              bq(
                                      dmq(must(), term("xyz", true)),
                                      dmq(must(), term("p1", true))
                              )
                         )
              ));
    }

    @Test
    public void testThatLeadingWildcardIsMatchedBetweenTwoFixedTerms() {

        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("abc *hemd def"),
                        synonym("$1", "hemd")
                )
        );

        ExpandedQuery query = makeQuery("abc xyzhemd def");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("abc", false),
                              bq(
                                      dmq(must(), term("xyz", true)),
                                      dmq(must(), term("hemd", true))
                              )
                         ),
                      dmq(
                              term("xyzhemd", false),
                              bq(
                                      dmq(must(), term("xyz", true)),
                                      dmq(must(), term("hemd", true))
                              )
                         ),
                      dmq(
                              term("def", false),
                              bq(
                                      dmq(must(), term("xyz", true)),
                                      dmq(must(), term("hemd", true))
                              )
                         )
              ));
    }

    @Test
    public void testThatLeadingWildcardDoesNotMatchWithoutRequiredLeftContext() {

        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("abc *hemd"),
                        synonym("$1", "hemd")
                )
        );

        ExpandedQuery query = makeQuery("xyzhemd");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("xyzhemd", false)
                         )
              ));
    }

    @Test
    public void testThatPlainRuleAndLeadingWildcardRuleBothApplyToSameTerm() {

        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("abc"),
                        synonym("klm")
                ),
                rule(
                        input("abc *hemd def"),
                        synonym("$1shirt")
                )
        );

        ExpandedQuery query = makeQuery("abc xyzhemd def");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("abc", false),
                              term("klm", true),
                              term("xyzshirt", true)
                         ),
                      dmq(
                              term("xyzhemd", false),
                              term("xyzshirt", true)
                         ),
                      dmq(
                              term("def", false),
                              term("xyzshirt", true)
                         )
              ));
    }
}
