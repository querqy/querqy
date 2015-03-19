package querqy.rewrite.commonrules.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static querqy.QuerqyMatchers.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;
import querqy.rewrite.commonrules.LineParser;

public class SynonymInstructionTest extends AbstractCommonRulesTest {
    
    final static Map<String, Object> EMPTY_CONTEXT = Collections.emptyMap();

    @Test
    public void testThatSingleTermIsExpandedWithSingleTerm() {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(Arrays.asList(mkTerm("s1")));
        builder.addRule(new Input(Arrays.asList(mkTerm("a"))), new Instructions(Arrays.asList((Instruction) synInstruction)));
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("a");
        Query rewritten = rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("a", false),
                              term("s1", true)
                              
                         )
              ));

    }
    
    @Test
    public void testThatTermInManyIsExpandedWithSingleTerm() throws Exception {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction1 = new SynonymInstruction(Arrays.asList(mkTerm("s1")));
        SynonymInstruction synInstruction2 = new SynonymInstruction(Arrays.asList(mkTerm("s2")));
        SynonymInstruction synInstruction3 = new SynonymInstruction(Arrays.asList(mkTerm("s3")));
        builder.addRule(new Input(Arrays.asList(mkTerm("a"))), new Instructions(Arrays.asList((Instruction) synInstruction1)));
        builder.addRule(new Input(Arrays.asList(mkTerm("b"))), new Instructions(Arrays.asList((Instruction) synInstruction2)));
        builder.addRule(new Input(Arrays.asList(mkTerm("c"))), new Instructions(Arrays.asList((Instruction) synInstruction3)));
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("a b c");
        Query rewritten = rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();

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
    public void testThatSingleTermIsExpandedByMany() throws Exception {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(Arrays.asList(mkTerm("s1_1"), mkTerm("s1_2")));
        builder.addRule(new Input(Arrays.asList(mkTerm("a"))), new Instructions(Arrays.asList((Instruction) synInstruction)));
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("a");
        Query rewritten = rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();

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
    public void testThatMultipleTermsAreExpandedBySingle() throws Exception {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(Arrays.asList(mkTerm("s1")));
        builder.addRule(new Input(Arrays.asList(mkTerm("a"), mkTerm("b"))), new Instructions(Arrays.asList((Instruction) synInstruction)));
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("a b");
        Query rewritten = rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();

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
    public void testThatMultipleTermsAreExpandedByMany() throws Exception {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(Arrays.asList(mkTerm( "s1_1"), mkTerm("s1_2")));
        builder.addRule(new Input(Arrays.asList(mkTerm("a"), mkTerm("b"))), new Instructions(Arrays.asList((Instruction) synInstruction)));
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("a b c");
        Query rewritten = rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();

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
    public void testThatPrefixIsMatchedAndPlaceHolderGetsReplaced() throws Exception {
        
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(Arrays.asList(mkTerm( "p1"), mkTerm("$1")));
        builder.addRule((Input) LineParser.parseInput("p1*"), new Instructions(Arrays.asList((Instruction) synInstruction)));
        
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("p1xyz");
        Query rewritten = rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();

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
    public void testThatWildcardDoesNotMatchZeroChars() throws Exception {
        
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(Arrays.asList(mkTerm( "p1"), mkTerm("$1")));
        builder.addRule((Input) LineParser.parseInput("p1*"), new Instructions(Arrays.asList((Instruction) synInstruction)));
        
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("p1");
        Query rewritten = rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("p1", false)
                         )
                         
                         
              ));
        
        
        
    }
    
    

}
