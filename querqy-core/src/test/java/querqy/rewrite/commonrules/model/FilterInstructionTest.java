package querqy.rewrite.commonrules.model;

import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import querqy.model.*;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static querqy.QuerqyMatchers.*;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.term;

/**
 * Created by rene on 08/12/2015.
 */
public class FilterInstructionTest  extends AbstractCommonRulesTest {

    @Test
    public void testThatBoostQueriesWithMustClauseUseMM100ByDefault() {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        FilterInstruction filterInstruction = new FilterInstruction(makeQuery("a b").getUserQuery());

        builder.addRule(new Input(Arrays.asList(mkTerm("x")), false, false), new Instructions(Arrays.asList((Instruction) filterInstruction)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("x");
        Collection<QuerqyQuery<?>> filterQueries = rewriter.rewrite(query, new EmptySearchRequestAdapter()).getFilterQueries();

        QuerqyQuery<?> qq = filterQueries.iterator().next();
        assertTrue(qq instanceof BooleanQuery);


        assertThat((BooleanQuery) qq,
                bq(
                    dmq(must(), term("a", true)),
                    dmq(must(), term("b", true))
                )
        );

    }


    @Test
    public void testPurelyNegativeFilterQuery() {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(true);

        FilterInstruction filterInstruction = new FilterInstruction(makeQuery("-ab").getUserQuery());

        builder.addRule(new Input(Collections.singletonList(mkTerm("x")), false, false),
                new Instructions(Collections.singletonList((Instruction) filterInstruction)));

        RulesCollection rules = builder.build();

        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("x");

        Collection<QuerqyQuery<?>> filterQueries = rewriter.rewrite(query, new EmptySearchRequestAdapter()).getFilterQueries();

        assertNotNull(filterQueries);
        assertEquals(1, filterQueries.size());

        QuerqyQuery<?> qq = filterQueries.iterator().next();
        assertTrue(qq instanceof BooleanQuery);

        assertThat((BooleanQuery) qq,
                bq(
                    should(),
                    dmq(
                            mustNot(),
                            term("ab", true)
                    )
                )
        );

    }

    @Test
    public void testThatFilterQueriesAreMarkedAsGenerated() {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        FilterInstruction filterInstruction = new FilterInstruction(makeQuery("a").getUserQuery());

        builder.addRule(new Input(Arrays.asList(mkTerm("x")), false, false), new Instructions(Arrays.asList((Instruction) filterInstruction)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("x");
        Collection<QuerqyQuery<?>> filterQueries = rewriter.rewrite(query, new EmptySearchRequestAdapter()).getFilterQueries();

        QuerqyQuery<?> qq = filterQueries.iterator().next();
        assertTrue(qq instanceof BooleanQuery);


        assertThat((BooleanQuery) qq,
                bq(
                        dmq(must(), term("a", true))
                )
        );

    }

    @Test
    public void testThatMainQueryIsNotMarkedAsGenerated() {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        FilterInstruction filterInstruction = new FilterInstruction(makeQuery("a").getUserQuery());

        builder.addRule(new Input(Arrays.asList(mkTerm("x")), false, false), new Instructions(Arrays.asList((Instruction) filterInstruction)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("x");
        QuerqyQuery<?> mainQuery = rewriter.rewrite(query, new EmptySearchRequestAdapter()).getUserQuery();
        assertFalse(mainQuery.isGenerated());

    }

}
