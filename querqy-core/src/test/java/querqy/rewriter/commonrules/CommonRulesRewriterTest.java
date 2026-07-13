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
package querqy.rewriter.commonrules;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.term;

import org.junit.Test;

import querqy.model.Clause;
import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.model.StringRawQuery;
import querqy.rewrite.RewriterOutput;
import querqy.rewriter.commonrules.AbstractCommonRulesTest;
import querqy.rewriter.commonrules.CommonRulesRewriter;

public class CommonRulesRewriterTest extends AbstractCommonRulesTest {

    @Test
    public void testInputBoundaryOnBothSides() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("\"a\""),
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
    
        query = makeQuery("a b");
        rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
        bq(
                dmq(
                        term("a", false)
                          
                     ),
                dmq(
                        term("b", false)
                               
                     )  
                     
          ));    
        
        query = makeQuery("b a");
        rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
        bq(
                dmq(
                        term("b", false)
                          
                     ),
                dmq(
                        term("a", false)
                               
                     )  
                     
          ));    
        
        
        query = makeQuery("b a c");
        rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
        bq(
                dmq(
                        term("b", false)
                          
                     ),
                dmq(
                        term("a", false)
                               
                     ),
                dmq(
                         term("c", false)
                                    
                     ) 
                     
          ));    

        
    }
    
    @Test
    public void testInputBoundaryOnLeftHandSide() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("\"a"),
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
    
        query = makeQuery("a b");
        rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
        bq(
                dmq(
                        term("a", false),
                        term("s1", true)
                     ),
                dmq(
                        term("b", false)
                               
                     )  
                     
          ));    
        
        query = makeQuery("b a");
        rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
        bq(
                dmq(
                        term("b", false)
                          
                     ),
                dmq(
                        term("a", false)
                               
                     )  
                     
          ));    
        
        
        query = makeQuery("b a c");
        rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
        bq(
                dmq(
                        term("b", false)
                          
                     ),
                dmq(
                        term("a", false)
                               
                     ),
                dmq(
                         term("c", false)
                                    
                     ) 
                     
          ));    
    }
    
    @Test
    public void testInputBoundaryOnRightHandSide() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("a\""),
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
    
        query = makeQuery("a b");
        rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
        bq(
                dmq(
                        term("a", false)
                     ),
                dmq(
                        term("b", false)
                               
                     )  
                     
          ));    
        
        query = makeQuery("b a");
        rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
        bq(
                dmq(
                        term("b", false)
                          
                     ),
                dmq(
                        term("a", false),
                        term("s1", true)
                               
                     )  
                     
          ));    
        
        
        query = makeQuery("b a c");
        rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
        bq(
                dmq(
                        term("b", false)
                          
                     ),
                dmq(
                        term("a", false)
                               
                     ),
                dmq(
                         term("c", false)
                                    
                     ) 
                     
          ));    
    }

    @Test
    public void testThatRawQueryAsUserQueryIsJustPassedUnchanged() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("a\""),
                        synonym("s1")
                )
        );

        final StringRawQuery userQuery = new StringRawQuery(null, "{!terms f=id}123", Clause.Occur.MUST, false);
        final ExpandedQuery query = new ExpandedQuery(userQuery);
        final RewriterOutput output = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter());
        assertEquals(userQuery, output.getExpandedQuery().getUserQuery());
    }
}
