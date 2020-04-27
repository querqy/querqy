package querqy.v2;

import org.junit.Test;
import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;
import querqy.rewrite.commonrules.model.DeleteInstruction;
import querqy.rewrite.commonrules.model.Input;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.RulesCollection;
import querqy.rewrite.commonrules.model.RulesCollectionBuilder;
import querqy.rewrite.commonrules.model.SynonymInstruction;
import querqy.rewrite.commonrules.model.TrieMapRulesCollectionBuilder;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.term;
import static querqy.rewrite.commonrules.select.SelectionStrategyFactory.DEFAULT_SELECTION_STRATEGY;

public class ExploreDelete extends AbstractCommonRulesTest {

    @Test
    public void testDeleteForSequence() {
        RulesCollectionBuilder synonymRulesBuilder = new TrieMapRulesCollectionBuilder(false);
        SynonymInstruction synInstruction = new SynonymInstruction(Collections.singletonList(mkTerm("s1")));
        synonymRulesBuilder.addRule(
                new Input(Collections.singletonList(mkTerm("a")), "a"),
                new Instructions(1, "1", Collections.singletonList(synInstruction)));

        SynonymInstruction synInstruction2 = new SynonymInstruction(Collections.singletonList(mkTerm("s2")));
        synonymRulesBuilder.addRule(
                new Input(Collections.singletonList(mkTerm("c")), "c"),
                new Instructions(2, "2", Collections.singletonList(synInstruction2)));

        RulesCollection rules = synonymRulesBuilder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("a b c d");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

        assertThat(rewritten,
                bq(
                        dmq(
                                term("a", false),
                                term("s1", true)

                        ),
                        dmq(
                                term("b", false)
                        ),
                        dmq(
                                term("c", false),
                                term("s2", true)
                        ),
                        dmq(
                                term("d", false)
                        )
                ));

        RulesCollectionBuilder deleteRuleBuilder = new TrieMapRulesCollectionBuilder(false);

        DeleteInstruction deleteInstruction = new DeleteInstruction(
                Arrays.asList(mkTerm("a"), mkTerm("b"), mkTerm("c")));
        deleteRuleBuilder.addRule(
                new Input(Arrays.asList(mkTerm("a"), mkTerm("b"), mkTerm("c")), "a b c"),
                new Instructions(3, "3", Collections.singletonList(deleteInstruction)));

        rules = deleteRuleBuilder.build();
        rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);
        rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

        assertThat(rewritten,
                bq(
                        dmq(
                                term("s1", true)

                        ),
                        dmq(
                                term("s2", true)
                        ),
                        dmq(
                                term("d", false)
                        )
                ));
    }

}
