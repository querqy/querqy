package querqy.rewrite.commonrules.model;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.term;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import querqy.Constants;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;

public class CommonRulesRewriterTest extends AbstractCommonRulesTest {

    @Test
    public void testInputBoundaryOnBothSides() {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(Arrays.asList(mkTerm("s1")));
        builder.addRule(new Input(Arrays.asList(mkTerm("a")), true, true), new Properties(new Instructions(Arrays.asList((Instruction) synInstruction))));
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, Constants.DEFAULT_SELECTION_STRATEDGY);

        ExpandedQuery query = makeQuery("a");
        Query rewritten = (Query) rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("a", false),
                              term("s1", true)
                              
                         )
              ));    
    
        query = makeQuery("a b");
        rewritten = (Query) rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();

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
        rewritten = (Query) rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();

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
        rewritten = (Query) rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();

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
        SynonymInstruction synInstruction = new SynonymInstruction(Arrays.asList(mkTerm("s1")));
        builder.addRule(new Input(Arrays.asList(mkTerm("a")), true, false), new Properties(new Instructions(Arrays.asList((Instruction) synInstruction))));
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, Constants.DEFAULT_SELECTION_STRATEDGY);

        ExpandedQuery query = makeQuery("a");
        Query rewritten = (Query) rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("a", false),
                              term("s1", true)
                              
                         )
              ));    
    
        query = makeQuery("a b");
        rewritten = (Query) rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();

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
        rewritten = (Query) rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();

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
        rewritten = (Query) rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();

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
        SynonymInstruction synInstruction = new SynonymInstruction(Arrays.asList(mkTerm("s1")));
        builder.addRule(new Input(Arrays.asList(mkTerm("a")), false, true), new Properties(new Instructions(Arrays.asList((Instruction) synInstruction))));
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, Constants.DEFAULT_SELECTION_STRATEDGY);

        ExpandedQuery query = makeQuery("a");
        Query rewritten = (Query) rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("a", false),
                              term("s1", true)
                              
                         )
              ));    
    
        query = makeQuery("a b");
        rewritten = (Query) rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();

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
        rewritten = (Query) rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();

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
        rewritten = (Query) rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();

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
        SynonymInstruction synInstructionA = new SynonymInstruction(Arrays.asList(mkTerm("aSynonym")));
        SynonymInstruction synInstructionB = new SynonymInstruction(Arrays.asList(mkTerm("bSynonym")));
        builder.addRule(new Input(Arrays.asList(mkTerm("a")), true, false), new Properties(new Instructions(Arrays.asList((Instruction) synInstructionA))));
        builder.addRule(new Input(Arrays.asList(mkTerm("b")), true, false), new Properties(new Instructions(Arrays.asList((Instruction) synInstructionB))));
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, Constants.DEFAULT_SELECTION_STRATEDGY);

        ExpandedQuery query = makeQuery("a b");
        Map<String, Object> context = new HashMap<>();
        Query rewritten = (Query) rewriter.rewrite(query, context).getUserQuery();

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

        assertThat(context.get(CommonRulesRewriter.CONTEXT_KEY_DEBUG_DATA), is(nullValue()));

        context.put(CommonRulesRewriter.CONTEXT_KEY_DEBUG_ENABLED, true);
        rewriter.rewrite(query, context).getUserQuery();

        assertThat(context.containsKey(CommonRulesRewriter.CONTEXT_KEY_DEBUG_DATA), is(true));
        assertThat(context.get(CommonRulesRewriter.CONTEXT_KEY_DEBUG_DATA).toString(), containsString(synInstructionA.toString()));
        assertThat(context.get(CommonRulesRewriter.CONTEXT_KEY_DEBUG_DATA).toString(), not(containsString(synInstructionB.toString())));
    }

}
