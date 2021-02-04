package querqy.rewrite.commonrules.model;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static querqy.QuerqyMatchers.*;
import static querqy.rewrite.commonrules.select.SelectionStrategyFactory.DEFAULT_SELECTION_STRATEGY;

import java.util.Arrays;

import org.junit.Test;

import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;
import querqy.model.Input;
import querqy.rewrite.commonrules.LineParser;

public class SynonymInstructionTest extends AbstractCommonRulesTest {
    
    @Test
    public void testThatSingleTermIsExpandedWithSingleTerm() {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(singletonList(mkTerm("s1")));
        builder.addRule(new Input.SimpleInput(singletonList(mkTerm("a")), "a"),
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

    }
    
    @Test
    public void testThatTermInManyIsExpandedWithSingleTerm() {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction1 = new SynonymInstruction(singletonList(mkTerm("s1")));
        SynonymInstruction synInstruction2 = new SynonymInstruction(singletonList(mkTerm("s2")));
        SynonymInstruction synInstruction3 = new SynonymInstruction(singletonList(mkTerm("s3")));
        builder.addRule(new Input.SimpleInput(singletonList(mkTerm("a")), "a"),
                new Instructions(1, "1", singletonList(synInstruction1)));
        builder.addRule(new Input.SimpleInput(singletonList(mkTerm("b")), "b"),
                new Instructions(2, "2", singletonList(synInstruction2)));
        builder.addRule(new Input.SimpleInput(singletonList(mkTerm("c")), "c"),
                new Instructions(3, "3", singletonList(synInstruction3)));
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("a b c");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

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
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(Arrays.asList(mkTerm("s1_1"), mkTerm("s1_2")));
        builder.addRule(new Input.SimpleInput(singletonList(mkTerm("a")), "a"),
                new Instructions(1, "1", singletonList(synInstruction)));
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("a");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

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
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(singletonList(mkTerm("s1")));
        builder.addRule(new Input.SimpleInput(Arrays.asList(mkTerm("a"), mkTerm("b")), "a b"),
                new Instructions(1, "1", singletonList( synInstruction)));
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("a b");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

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
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(Arrays.asList(mkTerm( "s1_1"), mkTerm("s1_2")));
        builder.addRule(new Input.SimpleInput(Arrays.asList(mkTerm("a"), mkTerm("b")), "a b"),
                new Instructions(1, "1", singletonList(synInstruction)));
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("a b c");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

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
        
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(Arrays.asList(mkTerm( "p1"), mkTerm("$1")));
        builder.addRule((Input.SimpleInput) LineParser.parseInput("p1*"),
                new Instructions(1, "1", singletonList(synInstruction)));
        
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("p1xyz");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

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
        
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(Arrays.asList(mkTerm( "bus"), mkTerm("$1")));
        builder.addRule((Input.SimpleInput) LineParser.parseInput("bus*"),
                new Instructions(1, "1", singletonList(synInstruction)));
        
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("busstop");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

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
        
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(Arrays.asList(mkTerm( "p2"), mkTerm("$1")));
        builder.addRule((Input.SimpleInput) LineParser.parseInput("p1 p2*"),
                new Instructions(1, "1", singletonList(synInstruction)));
        
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("p1 p2xyz");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

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
        
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(Arrays.asList(mkTerm( "p1"), mkTerm("$1")));
        builder.addRule((Input.SimpleInput) LineParser.parseInput("p1*"),
                new Instructions(1, "1", singletonList(synInstruction)));
        
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("p1");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("p1", false)
                         )
                         
                         
              ));
        
        
        
    }
    
    

}
