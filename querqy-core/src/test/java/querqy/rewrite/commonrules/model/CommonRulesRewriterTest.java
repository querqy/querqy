package querqy.rewrite.commonrules.model;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.term;
import static querqy.rewrite.commonrules.select.SelectionStrategyFactory.DEFAULT_SELECTION_STRATEGY;

import org.junit.Test;

import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;
import querqy.model.Input;

public class CommonRulesRewriterTest extends AbstractCommonRulesTest {

    @Test
    public void testInputBoundaryOnBothSides() {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(singletonList(mkTerm("s1")));
        builder.addRule(new Input.SimpleInput(singletonList(mkTerm("a")), true, true, "\"a\""),
                new Instructions(1, "1", singletonList(synInstruction)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("a");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("a", false),
                              term("s1", true)
                              
                         )
              ));    
    
        query = makeQuery("a b");
        rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

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
        rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

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
        rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

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
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(singletonList(mkTerm("s1")));
        builder.addRule(new Input.SimpleInput(singletonList(mkTerm("a")), true, false, "\"a"),
                new Instructions(1, "1", singletonList(synInstruction)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("a");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("a", false),
                              term("s1", true)
                              
                         )
              ));    
    
        query = makeQuery("a b");
        rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

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
        rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

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
        rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

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
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(singletonList(mkTerm("s1")));
        builder.addRule(new Input.SimpleInput(singletonList(mkTerm("a")), false, true, "a\""),
                new Instructions(1, "1", singletonList(synInstruction)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("a");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("a", false),
                              term("s1", true)
                              
                         )
              ));    
    
        query = makeQuery("a b");
        rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

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
        rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

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
        rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

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
    public void testActionsAreLoggedInContext() {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstructionA = new SynonymInstruction(singletonList(mkTerm("aSynonym")));
        SynonymInstruction synInstructionB = new SynonymInstruction(singletonList(mkTerm("bSynonym")));
        builder.addRule(new Input.SimpleInput(singletonList(mkTerm("a")), true, false, "\"a"),
                new Instructions(1, "1", singletonList(synInstructionA)));
        builder.addRule(new Input.SimpleInput(singletonList(mkTerm("b")), true, false, "\"a"),
                new Instructions(2, "2", singletonList(synInstructionB)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("a b");
        SearchEngineRequestAdapter searchEngineRequestAdapter = new EmptySearchEngineRequestAdapter();
        Query rewritten = (Query) rewriter.rewrite(query, searchEngineRequestAdapter).getUserQuery();

        assertThat(rewritten,
                bq(
                        dmq(
                                term("a", false),
                                term("aSynonym", true)
                        ),
                        dmq(
                                term("b", false)

                        )
                ));

        assertThat(searchEngineRequestAdapter.getContext().get(CommonRulesRewriter.CONTEXT_KEY_DEBUG_DATA),
                is(nullValue()));

        searchEngineRequestAdapter.getContext().put(CommonRulesRewriter.CONTEXT_KEY_DEBUG_ENABLED, true);
        rewriter.rewrite(query, searchEngineRequestAdapter).getUserQuery();

        assertThat(searchEngineRequestAdapter.getContext().containsKey(CommonRulesRewriter.CONTEXT_KEY_DEBUG_DATA),
                is(true));
        assertThat(searchEngineRequestAdapter.getContext().get(CommonRulesRewriter.CONTEXT_KEY_DEBUG_DATA).toString(),
                containsString(synInstructionA.toString()));
        assertThat(searchEngineRequestAdapter.getContext().get(CommonRulesRewriter.CONTEXT_KEY_DEBUG_DATA).toString(),
                not(containsString(synInstructionB.toString())));
    }

}
