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
    public void testOverlappingDeleteInstructions() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("a", "b"),
                        delete("a")
                ),
                rule(
                        input("a", "b"),
                        delete("a", "b")
                )
        );

        ExpandedQuery query = makeQuery("a b c");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
                bq(
                        dmq(
                                term("c", false)
                        )
                ));
    }

    @Test
    public void testSynonymBeforeDelete() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("a"),
                        synonym("c")
                ),
                rule(
                        input("a", "b"),
                        delete("a")
                )
        );

        ExpandedQuery query = makeQuery("a b");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
                bq(
                        dmq(
                                term("c", true)
                        ),
                        dmq(
                                term("b", false)
                        )
                ));

    }

    @Test
    public void testSynonymAfterDelete() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("a", "b"),
                        delete("a")
                ),
                rule(
                        input("a"),
                        synonym("c")
                )
        );

        ExpandedQuery query = makeQuery("a b");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
                bq(
                        dmq(
                                term("b", false)
                        )
                ));
    }

    @Test
    public void testExpandBySynonymAndDeleteBothInputTermsBySeparateInstructions() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("a", "b"),
                        delete("a"),
                        delete("b"),
                        synonym("s1")
                )
        );

        ExpandedQuery query = makeQuery("a b");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

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
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("a", "b"),
                        synonym("s1"),
                        delete("a", "b")
                )
        );

        ExpandedQuery query = makeQuery("a b");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

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
    public void testExpandBySynonymAndDeleteTwoOfThreeInputTermsBySingleInstruction() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("a", "b", "c"),
                        synonym("s1"),
                        delete("a", "b")
                )
        );

        ExpandedQuery query = makeQuery("a b c");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

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
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("a", "b"),
                        synonym("s1"),
                        delete("a"),
                        delete("b")
                ),
                rule(
                        input("a"),
                        synonym("s2")
                ),
                rule(
                        input("b"),
                        synonym("s3")
                )
        );

        ExpandedQuery query = makeQuery("a b");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

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
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("a", "b"),
                        synonym("s1_1", "s1_2"),
                        delete("a"),
                        delete("b")
                )
        );

        ExpandedQuery query = makeQuery("a b");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

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
