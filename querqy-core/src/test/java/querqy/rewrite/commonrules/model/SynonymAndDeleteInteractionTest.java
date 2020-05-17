package querqy.rewrite.commonrules.model;

import org.junit.Test;
import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;

import static org.hamcrest.MatcherAssert.assertThat;
import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.must;
import static querqy.QuerqyMatchers.term;
import static querqy.rewrite.commonrules.select.SelectionStrategyFactory.DEFAULT_SELECTION_STRATEGY;

public class SynonymAndDeleteInteractionTest extends AbstractCommonRulesTest {

    @Test
    public void testExpandBySynonymAndDeleteBothInputTermsBySeparateInstructions() {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        addRule(builder,
                input("a", "b"),
                synonym("s1"),
                delete("a"),
                delete("b")
        );

        CommonRulesRewriter rewriter = new CommonRulesRewriter(builder.build(), DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("a b");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

        assertThat(rewritten,
                bq(
                        dmq(
                                term("s1", true)

                        ),
                        dmq(
                                term("s1", true)
                        )
                ));

    }

    @Test
    public void testExpandBySynonymAndDeleteBothInputTermsBySingleInstruction() {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        addRule(builder,
                input("a", "b"),
                synonym("s1"),
                delete("a", "b")
        );

        CommonRulesRewriter rewriter = new CommonRulesRewriter(builder.build(), DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("a b");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

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
    public void testExpandBySynonymAndDeleteTwoOfThreeInputTermsBySingleInstruction() {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        addRule(builder,
                input("a", "b", "c"),
                synonym("s1"),
                delete("a", "b")
        );

        CommonRulesRewriter rewriter = new CommonRulesRewriter(builder.build(), DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("a b c");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

        assertThat(rewritten,
                bq(
                        dmq(
                                term("s1", true)

                        ),
                        dmq(
                                term("s1", true)

                        ),
                        dmq(
                                term("c", false),
                                term("s1", true)
                        )
                ));
    }

    @Test
    public void testSynonymsForTermsToBeDeleted() {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        addRule(builder,
                input("a", "b"),
                synonym("s1"),
                delete("a"),
                delete("b")
        );

        addRule(builder,
                input("a"),
                synonym("s2")
        );

        addRule(builder,
                input("b"),
                synonym("s3")
        );

        CommonRulesRewriter rewriter = new CommonRulesRewriter(builder.build(), DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("a b");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

        assertThat(rewritten,
                bq(
                        dmq(
                                term("s1", true),
                                term("s2", true)

                        ),
                        dmq(
                                term("s1", true),
                                term("s3", true)
                        )
                ));
    }

    @Test
    public void testExpandQueryByTwoSynonymTermsAndDeleteBothInputTerms() {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        addRule(builder,
                input("a", "b"),
                synonym("s1_1", "s1_2"),
                delete("a"),
                delete("b")
        );

        CommonRulesRewriter rewriter = new CommonRulesRewriter(builder.build(), DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("a b");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

        assertThat(rewritten,
                bq(
                        dmq(
                                bq(
                                        dmq(must(), term("s1_1", true)),
                                        dmq(must(), term("s1_2", true))
                                )

                        ),
                        dmq(
                                bq(
                                        dmq(must(), term("s1_1", true)),
                                        dmq(must(), term("s1_2", true))

                                )

                        )
                ));
    }
}
