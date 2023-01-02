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
}
