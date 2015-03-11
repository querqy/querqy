package querqy.rewrite.commonrules.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static querqy.QuerqyMatchers.boostQ;
import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.term;


import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

import querqy.model.BoostQuery;
import querqy.model.ExpandedQuery;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;
import querqy.rewrite.commonrules.model.BoostInstruction.BoostDirection;

public class BoostInstructionTest extends AbstractCommonRulesTest {

    @Test
    public void testThatBoostQueriesAreMarkedAsGenerated() {
        
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        
        BoostInstruction boostInstruction = new BoostInstruction(makeQuery("a b").getUserQuery(), BoostDirection.UP, 0.5f);
        builder.addRule(new Input(Arrays.asList(mkTerm("x")), false, false), new Instructions(Arrays.asList((Instruction) boostInstruction)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("x");
        Collection<BoostQuery> upQueries = rewriter.rewrite(query).getBoostUpQueries();

        assertThat(upQueries,
              contains( 
                      boostQ(
                              bq(
                                      dmq(
                                                  term("a", true)
                                          ),
                                      dmq( term("b", true))
                              ),
                              0.5f
                              
              )));

        
        
    }

}
