package querqy.rewrite.commonrules.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static querqy.QuerqyMatchers.*;

import java.util.Arrays;

import org.junit.Test;

import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;

public class SynonymInstructionTest extends AbstractCommonRulesTest {

    @Test
    public void testThatSingleTermIsExpandedWithSingleTerm() {
        RulesCollectionBuilder builder = new RulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(Arrays.asList(mkTerm("s1")));
        builder.addRule(new Input(Arrays.asList(mkTerm("a"))), new Instructions(Arrays.asList((Instruction) synInstruction)));
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("a");
        Query rewritten = rewriter.rewrite(query).getUserQuery();

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
        RulesCollectionBuilder builder = new RulesCollectionBuilder(false);
        SynonymInstruction synInstruction1 = new SynonymInstruction(Arrays.asList(mkTerm("s1")));
        SynonymInstruction synInstruction2 = new SynonymInstruction(Arrays.asList(mkTerm("s2")));
        SynonymInstruction synInstruction3 = new SynonymInstruction(Arrays.asList(mkTerm("s3")));
        builder.addRule(new Input(Arrays.asList(mkTerm("a"))), new Instructions(Arrays.asList((Instruction) synInstruction1)));
        builder.addRule(new Input(Arrays.asList(mkTerm("b"))), new Instructions(Arrays.asList((Instruction) synInstruction2)));
        builder.addRule(new Input(Arrays.asList(mkTerm("c"))), new Instructions(Arrays.asList((Instruction) synInstruction3)));
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("a b c");
        Query rewritten = rewriter.rewrite(query).getUserQuery();

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
        RulesCollectionBuilder builder = new RulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(Arrays.asList(mkTerm("s1_1"), mkTerm("s1_2")));
        builder.addRule(new Input(Arrays.asList(mkTerm("a"))), new Instructions(Arrays.asList((Instruction) synInstruction)));
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("a");
        Query rewritten = rewriter.rewrite(query).getUserQuery();

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
        RulesCollectionBuilder builder = new RulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(Arrays.asList(mkTerm("s1")));
        builder.addRule(new Input(Arrays.asList(mkTerm("a"), mkTerm("b"))), new Instructions(Arrays.asList((Instruction) synInstruction)));
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("a b");
        Query rewritten = rewriter.rewrite(query).getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("a", false),
                              bq(
                                      dmq(must(), term("s1", true)),
                                      bq(mustNot(),
                                              dmq(must(), term("a", true)),
                                              dmq(must(), term("b", true))
                                              )
                              )
                              
                         ),
                         dmq(
                                 term("b", false),
                                 bq(
                                         dmq(must(), term("s1", true)),
                                         bq(mustNot(),
                                                 dmq(must(), term("a", true)),
                                                 dmq(must(), term("b", true))
                                                 )
                                 )
                                 
                            )
                         
                         
              ));
    }
    
    @Test
    public void testThatMultipleTermsAreExpandedByMany() throws Exception {
        RulesCollectionBuilder builder = new RulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(Arrays.asList(mkTerm( "s1_1"), mkTerm("s1_2")));
        builder.addRule(new Input(Arrays.asList(mkTerm("a"), mkTerm("b"))), new Instructions(Arrays.asList((Instruction) synInstruction)));
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("a b c");
        Query rewritten = rewriter.rewrite(query).getUserQuery();

        assertThat(rewritten,
              bq(
                      dmq(
                              term("a", false),
                              bq(
                                      dmq(must(), term("s1_1", true)),
                                      dmq(must(), term("s1_2", true)),
                                      bq(mustNot(),
                                              dmq(must(), term("a", true)),
                                              dmq(must(), term("b", true))
                                              )
                              )
                              
                         ),
                         dmq(
                                 term("b", false),
                                 bq(
                                         dmq(must(), term("s1_1", true)),
                                         dmq(must(), term("s1_2", true)),
                                         bq(mustNot(),
                                                 dmq(must(), term("a", true)),
                                                 dmq(must(), term("b", true))
                                                 )
                                 )
                                 
                            ),
                         dmq(term("c", false))
                         
                         
              ));
    }

}
