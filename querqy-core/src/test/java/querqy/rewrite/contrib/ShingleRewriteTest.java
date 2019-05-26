package querqy.rewrite.contrib;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import querqy.model.*;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;
import querqy.rewrite.commonrules.LineParser;
import querqy.rewrite.commonrules.model.Input;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.InstructionsTestSupport;
import querqy.rewrite.commonrules.model.RulesCollection;
import querqy.rewrite.commonrules.model.RulesCollectionBuilder;
import querqy.rewrite.commonrules.model.SynonymInstruction;
import querqy.rewrite.commonrules.model.TrieMapRulesCollectionBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static querqy.QuerqyMatchers.*;
import static querqy.rewrite.commonrules.SelectionStrategyFactory.DEFAULT_SELECTION_STRATEGY;
import static querqy.rewrite.commonrules.model.InstructionsTestSupport.instructions;

/**
 * Test for ShingleRewriter.
 */
public class ShingleRewriteTest extends AbstractCommonRulesTest {

    @Test
    public void testShinglingForTwoTokens() {
        Query query = new Query();
        addTerm(query, "cde");
        addTerm(query, "ajk");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ShingleRewriter rewriter = new ShingleRewriter();
        rewriter.rewrite(expandedQuery);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("cde"),
                                term("cdeajk")
                        ),
                        dmq(
                                term("ajk"),
                                term("cdeajk")
                        )
                )
        );
    }

    @Test
    public void testThatShinglingDoesNotTriggerExceptionOnSingleTerm() {
        Query query = new Query();
        addTerm(query, "t1");
        
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ShingleRewriter rewriter = new ShingleRewriter();
        rewriter.rewrite(expandedQuery);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("t1")
                        )
                )
        );
    }

    @Test
    public void testShinglingForTwoTokensWithSameField() {
        Query query = new Query();
        addTerm(query, "f1", "cde");
        addTerm(query, "f1", "ajk");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ShingleRewriter rewriter = new ShingleRewriter();
        rewriter.rewrite(expandedQuery);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("f1", "cde"),
                                term("f1", "cdeajk")
                        ),
                        dmq(
                                term("f1", "ajk"),
                                term("f1", "cdeajk")
                        )
                )
        );
    }

    @Test
    public void testShinglingForTwoTokensWithSameFieldAndGeneratedFlag() {
        Query query = new Query();
        addTerm(query, "f1", "cde", true);
        addTerm(query, "f1", "ajk", true);
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ShingleRewriter rewriter = new ShingleRewriter(true);
        rewriter.rewrite(expandedQuery);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("f1", "cde"),
                                term("f1", "cdeajk")
                        ),
                        dmq(
                                term("f1", "ajk"),
                                term("f1", "cdeajk")
                        )
                )
        );
    }

    @Test
    public void testShinglingForTwoTokensWithDifferentFieldsDontShingle() {
        Query query = new Query();
        addTerm(query, "f1", "cde");
        addTerm(query, "f2", "ajk");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ShingleRewriter rewriter = new ShingleRewriter();
        rewriter.rewrite(expandedQuery);

        assertThat((Query) expandedQuery.getUserQuery(), bq(dmq(term("cde")), dmq(term("ajk"))));
    }
    
    @Test
    public void testShinglingForTwoTokensWithOnFieldNameNullDontShingle() {
        Query query = new Query();
        addTerm(query, "f1", "cde");
        addTerm(query, "ajk");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ShingleRewriter rewriter = new ShingleRewriter();
        rewriter.rewrite(expandedQuery);

        assertThat((Query) expandedQuery.getUserQuery(), bq(dmq(term("f1", "cde")), dmq(term("ajk"))));
    }

    @Test
    public void testShinglingForThreeTokens() {
        Query query = new Query();
        addTerm(query, "cde");
        addTerm(query, "ajk");
        addTerm(query, "xyz");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ShingleRewriter rewriter = new ShingleRewriter();
        rewriter.rewrite(expandedQuery);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("cde"),
                                term("cdeajk")
                        ),
                        dmq(
                                term("ajk"),
                                term("cdeajk"),
                                term("ajkxyz")
                        ),
                        dmq(
                                term("xyz"),
                                term("ajkxyz")
                        )
                )
        );
    }

    @Test
    public void testShinglingForThreeTokensWithThreeTokenGenerated() {
        Query query = new Query();
        addTerm(query, "cde", true);
        addTerm(query, "ajk", true);
        addTerm(query, "xyz", true);
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ShingleRewriter rewriter = new ShingleRewriter(true);
        rewriter.rewrite(expandedQuery);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("cde"),
                                term("cdeajk")
                        ),
                        dmq(
                                term("ajk"),
                                term("cdeajk"),
                                term("ajkxyz")
                        ),
                        dmq(
                                term("xyz"),
                                term("ajkxyz")
                        )
                )
        );
    }



    @Test
    public void testShinglingForThreeTokensWithOneTokenGeneratedIgnoringGenerated() {
        Query query = new Query();
        addTerm(query, "cde", false);
        addTerm(query, "ajk", false);
        addTerm(query, "xyz", true);
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ShingleRewriter rewriter = new ShingleRewriter(false);
        rewriter.rewrite(expandedQuery);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("cde"),
                                term("cdeajk")
                        ),
                        dmq(
                                term("ajk"),
                                term("cdeajk")
                        ),
                        dmq(
                                term("xyz")
                        )
                )
        );
    }


    @Test
    public void testShinglingForThreeTokensWithMixedFields() {
        Query query = new Query();
        addTerm(query, "f1", "cde");
        addTerm(query, "f1", "ajk");
        addTerm(query, "f2", "xyz");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ShingleRewriter rewriter = new ShingleRewriter();
        rewriter.rewrite(expandedQuery);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("f1", "cde"),
                                term("f1", "cdeajk")
                        ),
                        dmq(    
                                term("f1", "ajk"),
                                term("f1", "cdeajk")
                        ),
                        dmq(term("f2", "xyz"))
                )
        );
    }
    
    @Test
    public void testChainingWithWildCard() {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(Arrays.asList(mkTerm( "p1"), mkTerm("$1")));
        builder.addRule((Input) LineParser.parseInput("p1*"),
                instructions(1, Collections.singletonList(synInstruction)));
        
        RulesCollection rules = builder.build();
        CommonRulesRewriter commonRulesRewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);
        ShingleRewriter shingleRewriter = new ShingleRewriter(false);

        ExpandedQuery query = makeQuery("p1xyz t2");
        query = commonRulesRewriter.rewrite(query, new EmptySearchEngineRequestAdapter());
        query = shingleRewriter.rewrite(query);
        
        assertThat((Query) query.getUserQuery(),
                bq(
                        dmq(
                                term("p1xyz", false),
                                bq(
                                        dmq(must(), term ("p1", true)),
                                        dmq(must(), term ("xyz", true))
                                        
                                        ),
                                term("p1xyzt2", true)
                        ),
                        dmq(
                                term("t2", false),
                                term("p1xyzt2", true)
                                )
                )
        );
        
        

    }
    
    @Test
    public void testShingleWithHyphens() {
        
        Query query = new Query();
        addTerm(query, "cde-fgh", false);
        addTerm(query, "-", false);
        addTerm(query, "xyz", false);
        
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ShingleRewriter rewriter = new ShingleRewriter(false);
        ExpandedQuery rewritten = rewriter.rewrite(expandedQuery);
        assertThat((Query) rewritten.getUserQuery(),
                bq(
                        dmq(
                                term("cde-fgh", false),
                                term ("cde-fgh-", true)
                        ),
                        dmq(
                                term("-", false),
                                term("cde-fgh-", true),
                                term("-xyz", true)
                                ),
                        dmq(
                                term("xyz", false),
                                term ("-xyz", true)
                        )
                                
                )
        );
        
    }

    @Test
    public void testVisitDMQCanHandleNoNonGeneratedClause() {
        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(null, Clause.Occur.SHOULD, false);

        // we cannot use Mockito mock objects as Mockito doesn't mock Term.isGenerated() (due to the primitive boolean
        // return type)
        Term term1 = new Term(dmq, null, "value1", true) {
            @Override
            public <T> T accept(NodeVisitor<T> visitor) {
                Assert.fail("Generated node must not be visited");
                return null;
            }
        };
        dmq.addClause(term1);

        Term term2 = new Term(dmq, null, "value2", true) {
            @Override
            public <T> T accept(NodeVisitor<T> visitor) {
                Assert.fail("Generated node must not be visited");
                return null;
            }
        };
        dmq.addClause(term2);

        ShingleRewriter rewriter = new ShingleRewriter(false);
        rewriter.visit(dmq);

    }

    private void addTerm(Query query, String value) {
        addTerm(query, null, value);
    }

    private void addTerm(Query query, String field, String value) {
        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, true);
        query.addClause(dmq);
        Term term = new Term(dmq, field, value);
        dmq.addClause(term);
    }

    private void addTerm(Query query, String value, boolean isGenerated) {
        addTerm(query, null, value, isGenerated);
    }

    private void addTerm(Query query, String field, String value, boolean isGenerated) {
        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, true);
        query.addClause(dmq);
        Term term = new Term(dmq, field, value, isGenerated);
        dmq.addClause(term);
    }

}
