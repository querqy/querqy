package querqy.rewrite.commonrules;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.term;
import static querqy.rewrite.commonrules.select.SelectionStrategyFactory.DEFAULT_SELECTION_STRATEGY;

import org.junit.Test;

import querqy.model.Clause;
import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.model.StringRawQuery;
import querqy.rewrite.RewriterOutput;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;
import querqy.model.Input;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.RulesCollection;
import querqy.rewrite.commonrules.model.RulesCollectionBuilder;
import querqy.rewrite.commonrules.model.SynonymInstruction;
import querqy.rewrite.commonrules.model.TrieMapRulesCollectionBuilder;

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
