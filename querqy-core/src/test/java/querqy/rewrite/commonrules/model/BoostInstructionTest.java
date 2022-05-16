package querqy.rewrite.commonrules.model;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertFalse;
import static querqy.QuerqyMatchers.boostQ;
import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.must;
import static querqy.QuerqyMatchers.mustNot;
import static querqy.QuerqyMatchers.term;
import static querqy.rewrite.commonrules.select.SelectionStrategyFactory.DEFAULT_SELECTION_STRATEGY;

import java.util.*;

import org.junit.Assert;
import org.junit.Test;

import querqy.model.*;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;
import querqy.model.Input;
import querqy.rewrite.commonrules.LineParser;
import querqy.rewrite.commonrules.model.BoostInstruction.BoostDirection;
import querqy.rewrite.commonrules.model.BoostInstruction.BoostMethod;

public class BoostInstructionTest extends AbstractCommonRulesTest {
    
    @Test
    public void testThatBoostQueriesAreMarkedAsGenerated() {
        
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        
        BoostInstruction boostInstruction = new BoostInstruction(makeQuery("a").getUserQuery(), BoostDirection.UP,
                BoostMethod.ADDITIVE, 0.5f);
        builder.addRule(new Input.SimpleInput(singletonList(mkTerm("x")), false, false, "x"),
                new Instructions(1, "1", singletonList(boostInstruction)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("x");
        Collection<BoostQuery> upQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getBoostUpQueries();

        assertThat(upQueries,
              contains( 
                      boostQ(
                              bq(
                                      dmq(must(), term("a", true))
                              ),
                              0.5f
                              
              )));

        
        
    }

    @Test
    public void testThatBoostQueriesUseMM100ByDefault() {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        BoostInstruction boostInstruction = new BoostInstruction(makeQuery("a b").getUserQuery(),
                BoostDirection.UP, BoostMethod.ADDITIVE, 0.5f);
        builder.addRule(new Input.SimpleInput(singletonList(mkTerm("x")), false, false, "x"),
                new Instructions(1, "1", singletonList(boostInstruction)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("x");
        Collection<BoostQuery> upQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getBoostUpQueries();

        assertThat(upQueries,
                contains(
                        boostQ(
                                bq(
                                        dmq(must(), term("a", true)),
                                        dmq(must(), term("b", true))
                                ),
                                0.5f

                        )));



    }

    @Test
    public void testThatBoostQueriesWithMustClauseUseMM100ByDefault() {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        BoostInstruction boostInstruction = new BoostInstruction(makeQuery("a +b").getUserQuery(), BoostDirection.UP,
                BoostMethod.ADDITIVE,0.5f);
        builder.addRule(new Input.SimpleInput(singletonList(mkTerm("x")), false, false, "x"),
                new Instructions(1, "1", singletonList(boostInstruction)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("x");
        Collection<BoostQuery> upQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getBoostUpQueries();

        assertThat(upQueries,
                contains(
                        boostQ(
                                bq(
                                        dmq(must(), term("a", true)),
                                        dmq(must(), term("b", true))
                                ),
                                0.5f

                        )));



    }

    @Test
    public void testThatBoostQueriesWithMustNotClauseUseMM100ByDefault() {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        BoostInstruction boostInstruction = new BoostInstruction(makeQuery("-a b").getUserQuery(),
                BoostDirection.UP, BoostMethod.ADDITIVE,0.5f);
        builder.addRule(new Input.SimpleInput(singletonList(mkTerm("x")), false, false, "x"),
                new Instructions(1, "1", singletonList(boostInstruction)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("x");
        Collection<BoostQuery> upQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getBoostUpQueries();

        assertThat(upQueries,
                contains(
                        boostQ(
                                bq(
                                        dmq(mustNot(), term("a", true)),
                                        dmq(must(), term("b", true))
                                ),
                                0.5f

                        )));



    }

    @Test
    public void testPurelyNegativeBoostQuery() {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        BoostInstruction boostInstruction = new BoostInstruction(makeQuery("-a").getUserQuery(), BoostDirection.UP,
                BoostMethod.ADDITIVE,0.5f);

        builder.addRule(new Input.SimpleInput(singletonList(mkTerm("x")), false, false, "x"),
                new Instructions(1, "1", singletonList(boostInstruction)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("x");
        Collection<BoostQuery> upQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getBoostUpQueries();

        assertThat(upQueries,
                contains(
                        boostQ(
                                bq(
                                        dmq(mustNot(), term("a", true))
                                ),
                                0.5f

                        )));


    }

    @Test
    public void testMultiplicativeBoostQuery() {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        BoostInstruction boostInstruction = new BoostInstruction(makeQuery("a").getUserQuery(), BoostDirection.UP,
                BoostMethod.MULTIPLICATIVE,2f);

        builder.addRule(new Input.SimpleInput(singletonList(mkTerm("x")), false, false, "x"),
                new Instructions(1, "1", singletonList(boostInstruction)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("x");
        Collection<BoostQuery> multiplicativeBoostQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getMultiplicativeBoostQueries();

        assertThat(multiplicativeBoostQueries,
                contains(
                        boostQ(
                                bq(
                                        dmq(must(), term("a", true))
                                ),
                                2f

                        )));
    }

    @Test
    public void testMultiplicativeDownBoost() {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        BoostInstruction boostInstruction = new BoostInstruction(makeQuery("a").getUserQuery(), BoostDirection.DOWN,
                BoostMethod.MULTIPLICATIVE,2f);

        builder.addRule(new Input.SimpleInput(singletonList(mkTerm("x")), false, false, "x"),
                new Instructions(1, "1", singletonList(boostInstruction)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("x");
        Collection<BoostQuery> multiplicativeBoostQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getMultiplicativeBoostQueries();

        assertThat(multiplicativeBoostQueries,
                contains(
                        boostQ(
                                bq(
                                        dmq(must(), term("a", true))
                                ),
                                0.5f

                        )));
    }


    @Test
    public void testThatMainQueryIsNotMarkedAsGenerated() {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        BoostInstruction boostInstruction = new BoostInstruction(makeQuery("-a b").getUserQuery(), BoostDirection.UP,
                BoostMethod.ADDITIVE, 0.5f);
        builder.addRule(new Input.SimpleInput(singletonList(mkTerm("x")), false, false, "x"),
                new Instructions(1, "1", singletonList(boostInstruction)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("x");
        QuerqyQuery<?> mainQuery = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

        assertFalse(mainQuery.isGenerated());

    }

    @Test
    public void testThatUpQueriesAreOfTypeQuery() {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        BoostInstruction boostInstruction = new BoostInstruction(makeQuery("a b").getUserQuery(), BoostDirection.UP,
                BoostMethod.ADDITIVE, 0.5f);
        builder.addRule(new Input.SimpleInput(singletonList(mkTerm("x")), false, false, "x"),
                new Instructions(1, "1", singletonList(boostInstruction)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("x");
        Collection<BoostQuery> upQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getBoostUpQueries();
        for (BoostQuery bq : upQueries) {
            Assert.assertTrue(bq.getQuery() instanceof Query);
        }

    }

    @Test
    public void testThatDownQueriesAreOfTypeQuery() {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        BoostInstruction boostInstruction = new BoostInstruction(makeQuery("a b$1").getUserQuery(), BoostDirection.DOWN,
                BoostMethod.ADDITIVE, 0.2f);

        builder.addRule((Input.SimpleInput) LineParser.parseInput("x k*"), new Instructions(1, "1",
                singletonList(boostInstruction)));


        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);


        ExpandedQuery query = makeQuery("x klm y");


        Collection<BoostQuery> downQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getBoostDownQueries();


        for (BoostQuery bq : downQueries) {
            Assert.assertTrue(bq.getQuery() instanceof Query);
        }

    }

    @Test
    public void testThatPlaceHolderGetsReplaced() {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        BoostInstruction boostInstruction = new BoostInstruction(makeQuery("a b$1").getUserQuery(), BoostDirection.DOWN,
                BoostMethod.ADDITIVE, 0.2f);

        builder.addRule((Input.SimpleInput) LineParser.parseInput("x k*"), new Instructions(1, "1",
                singletonList(boostInstruction)));


        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);


        ExpandedQuery query = makeQuery("x klm y");


        Collection<BoostQuery> downQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getBoostDownQueries();

        assertThat(downQueries,
                contains(
                        boostQ(
                                bq(
                                        dmq(must(), term("a", true)),
                                        dmq(must(), term("blm", true))
                                ),
                                0.2f

                        )));







    }


    @Test
    public void testThatPlaceHolderGetsReplacedAsASeperateToken() {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        BoostInstruction boostInstruction = new BoostInstruction(makeQuery("a $1").getUserQuery(), BoostDirection.DOWN,
                BoostMethod.ADDITIVE, 0.3f);

        builder.addRule((Input.SimpleInput) LineParser.parseInput("x k*"), new Instructions(1, "1",
                singletonList(boostInstruction)));


        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);


        ExpandedQuery query = makeQuery("x klm y");


        Collection<BoostQuery> downQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getBoostDownQueries();

        assertThat(downQueries,
                contains(
                        boostQ(
                                bq(
                                        dmq(must(), term("a", true)),
                                        dmq(must(), term("lm", true))
                                ),
                                0.3f

                        )));







    }

    @Test
    public void testThatPlaceHolderGetsReplacedAsAnInfix() {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        BoostInstruction boostInstruction = new BoostInstruction(makeQuery("a c$1d").getUserQuery(), BoostDirection.UP,
                BoostMethod.ADDITIVE, 0.3f);

        builder.addRule((Input.SimpleInput) LineParser.parseInput("k*"), new Instructions(1, "1",
                singletonList(boostInstruction)));


        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);


        ExpandedQuery query = makeQuery("x klm y");


        Collection<BoostQuery> upQueries = rewriter.rewrite(query, new EmptySearchEngineRequestAdapter())
                .getBoostUpQueries();

        assertThat(upQueries,
                contains(
                        boostQ(
                                bq(
                                        dmq(must(), term("a", true)),
                                        dmq(must(), term("clmd", true))
                                ),
                                0.3f

                        )));







    }
}
