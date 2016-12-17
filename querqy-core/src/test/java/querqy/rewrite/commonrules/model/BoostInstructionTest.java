package querqy.rewrite.commonrules.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static querqy.QuerqyMatchers.boostQ;
import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.term;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import querqy.model.BoostQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;
import querqy.rewrite.commonrules.LineParser;
import querqy.rewrite.commonrules.model.BoostInstruction.BoostDirection;

public class BoostInstructionTest extends AbstractCommonRulesTest {
    
    final static Map<String, Object> EMPTY_CONTEXT = Collections.emptyMap();

    @Test
    public void testThatBoostQueriesAreMarkedAsGenerated() {
        
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        
        BoostInstruction boostInstruction = new BoostInstruction(makeQuery("a b").getUserQuery(), BoostDirection.UP, 0.5f);
        builder.addRule(new Input(Arrays.asList(mkTerm("x")), false, false), new Instructions(Arrays.asList((Instruction) boostInstruction)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("x");
        Collection<BoostQuery> upQueries = rewriter.rewrite(query, EMPTY_CONTEXT).getBoostUpQueries();

        assertThat(upQueries,
              contains( 
                      boostQ(
                              bq(
                                      dmq(term("a", true)),
                                      dmq( term("b", true))
                              ),
                              0.5f
                              
              )));

        
        
    }

    @Test
    public void testThatUpQueriesAreOfTypeQuery() throws Exception {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        BoostInstruction boostInstruction = new BoostInstruction(makeQuery("a b").getUserQuery(), BoostDirection.UP, 0.5f);
        builder.addRule(new Input(Arrays.asList(mkTerm("x")), false, false), new Instructions(Arrays.asList((Instruction) boostInstruction)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("x");
        Collection<BoostQuery> upQueries = rewriter.rewrite(query, EMPTY_CONTEXT).getBoostUpQueries();
        for (BoostQuery bq : upQueries) {
            Assert.assertTrue(bq.getQuery() instanceof Query);
        }

    }

    @Test
    public void testThatDownQueriesAreOfTypeQuery() throws Exception {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        BoostInstruction boostInstruction = new BoostInstruction(makeQuery("a b$1").getUserQuery(), BoostDirection.DOWN, 0.2f);

        builder.addRule((Input) LineParser.parseInput("x k*"), new Instructions(Collections.singletonList((Instruction) boostInstruction)));


        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);


        ExpandedQuery query = makeQuery("x klm y");


        Collection<BoostQuery> downQueries = rewriter.rewrite(query, EMPTY_CONTEXT).getBoostDownQueries();


        for (BoostQuery bq : downQueries) {
            Assert.assertTrue(bq.getQuery() instanceof Query);
        }

    }

    @Test
    public void testThatPlaceHolderGetsReplaced() throws Exception {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        BoostInstruction boostInstruction = new BoostInstruction(makeQuery("a b$1").getUserQuery(), BoostDirection.DOWN, 0.2f);

        builder.addRule((Input) LineParser.parseInput("x k*"), new Instructions(Collections.singletonList((Instruction) boostInstruction)));


        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);


        ExpandedQuery query = makeQuery("x klm y");


        Collection<BoostQuery> downQueries = rewriter.rewrite(query, EMPTY_CONTEXT).getBoostDownQueries();

        assertThat(downQueries,
                contains(
                        boostQ(
                                bq(
                                        dmq( term("a", true)),
                                        dmq( term("blm", true))
                                ),
                                0.2f

                        )));







    }


    @Test
    public void testThatPlaceHolderGetsReplacedAsASeperateToken() throws Exception {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        BoostInstruction boostInstruction = new BoostInstruction(makeQuery("a $1").getUserQuery(), BoostDirection.DOWN, 0.3f);

        builder.addRule((Input) LineParser.parseInput("x k*"), new Instructions(Collections.singletonList((Instruction) boostInstruction)));


        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);


        ExpandedQuery query = makeQuery("x klm y");


        Collection<BoostQuery> downQueries = rewriter.rewrite(query, EMPTY_CONTEXT).getBoostDownQueries();

        assertThat(downQueries,
                contains(
                        boostQ(
                                bq(
                                        dmq( term("a", true)),
                                        dmq( term("lm", true))
                                ),
                                0.3f

                        )));







    }

    @Test
    public void testThatPlaceHolderGetsReplacedAsAnInfix() throws Exception {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        BoostInstruction boostInstruction = new BoostInstruction(makeQuery("a c$1d").getUserQuery(), BoostDirection.UP, 0.3f);

        builder.addRule((Input) LineParser.parseInput("k*"), new Instructions(Collections.singletonList((Instruction) boostInstruction)));


        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);


        ExpandedQuery query = makeQuery("x klm y");


        Collection<BoostQuery> upQueries = rewriter.rewrite(query, EMPTY_CONTEXT).getBoostUpQueries();

        assertThat(upQueries,
                contains(
                        boostQ(
                                bq(
                                        dmq( term("a", true)),
                                        dmq( term("clmd", true))
                                ),
                                0.3f

                        )));







    }
}
