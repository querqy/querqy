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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static querqy.QuerqyMatchers.*;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.term;

/**
 * Created by rene on 08/12/2015.
 */
public class FilterInstructionTest  extends AbstractCommonRulesTest {

    final static Map<String, Object> EMPTY_CONTEXT = Collections.emptyMap();

    @Test
    public void testPurelyNegativeFilterQuery() {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(true);

        FilterInstruction filterInstruction = new FilterInstruction(makeQuery("-ab").getUserQuery());

        builder.addRule(new Input(Collections.singletonList(mkTerm("x")), false, false),
                new Instructions(Collections.singletonList((Instruction) filterInstruction)));

        RulesCollection rules = builder.build();

        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("x");

        Collection<QuerqyQuery<?>> filterQueries = rewriter.rewrite(query, EMPTY_CONTEXT).getFilterQueries();

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
}
