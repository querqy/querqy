package querqy.rewrite.commonrules.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static querqy.QuerqyMatchers.*;
import static querqy.rewrite.commonrules.select.SelectionStrategyFactory.DEFAULT_SELECTION_STRATEGY;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import querqy.model.*;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;
import querqy.model.Input;
import querqy.rewrite.commonrules.LineParser;

public class DeleteInstructionTest extends AbstractCommonRulesTest {

    @Test
    public void testThatLastTermIsDeleted() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("a"),
                        delete("a")
                )
        );

        ExpandedQuery query = makeQuery("a");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();
        assertTrue(rewritten.getClauses().isEmpty());
    }

    @Test
    public void testThatTermIsRemovedIfThereIsAnotherTermInTheSameDMQ() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("a"),
                        delete("a")
                )
        );

        ExpandedQuery expandedQuery = makeQuery("a");
        Query query = (Query) expandedQuery.getUserQuery();

        DisjunctionMaxQuery dmq = query.getClauses(DisjunctionMaxQuery.class).get(0);
        querqy.model.Term termB = new querqy.model.Term(dmq, null, "b");
        dmq.addClause(termB);

        Query rewritten = (Query) rewriter.rewrite(expandedQuery, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
                bq(
                        dmq(
                                term("b")
                        )
                ));
    }

    @Test
    public void testThatTermIsRemovedIfThereASecondDMQWithoutTheTerm() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("a"),
                        delete("a")
                )
        );

        Query rewritten = (Query) rewriter.rewrite(makeQuery("a b"), new EmptySearchEngineRequestAdapter())
                .getExpandedQuery().getUserQuery();

        assertThat(rewritten,
                bq(
                        dmq(
                                term("b")
                        )
                ));
    }

    @Test
    public void testThatAllTermsAreRemovedEvenIfASecondDMQWithTheSameTermAndNoOtherTermExists() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("a"),
                        delete("a")
                )
        );

        Query rewritten = (Query) rewriter.rewrite(makeQuery("a a"), new EmptySearchEngineRequestAdapter())
                .getExpandedQuery().getUserQuery();

        assertThat(rewritten, bq());
    }


    @Test
    public void testThatDeleteIsAppliedToWildcardInput() {

        final Input.SimpleInput input = input("k*");
        final CommonRulesRewriter rewriter = rewriter(rule(input,
                delete(input.getInputTerms())));

        ExpandedQuery query = makeQuery("x klm");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();
        assertThat(rewritten,
                bq(
                        dmq(
                                term("x")
                        )
                )
        );
    }

    @Test
    public void testThatDeleteIsAppliedToMultiTermWildcardInput() {

        final Input.SimpleInput input = input("ab k*");
        final CommonRulesRewriter rewriter = rewriter(rule(input,
                delete(input.getInputTerms())));

        ExpandedQuery query = makeQuery("x ab klm");

        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
                bq(
                        dmq(
                                term("x")
                        )
                ));
    }

    @Test
    public void testThatWilcardTermIsDeletedEvenIfItIsTheOnlyQueryTerm() {

        final Input.SimpleInput input = input("k*");
        final CommonRulesRewriter rewriter = rewriter(rule(input,
                delete(input.getInputTerms())));

        ExpandedQuery query = makeQuery("klm");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getExpandedQuery().getUserQuery();

        assertThat(rewritten,
                bq());
    }

    @Test
    public void testHashCode() {

        DeleteInstruction delete1 = new DeleteInstruction(Arrays.asList(mkTerm("a"), mkTerm("b")));
        DeleteInstruction delete2 = new DeleteInstruction(Arrays.asList(mkTerm("a"), mkTerm("b")));
        DeleteInstruction delete3 = new DeleteInstruction(Arrays.asList(mkTerm("a"), mkTerm("c")));
        DeleteInstruction delete4 = new DeleteInstruction(Arrays.asList(mkTerm("c"), mkTerm("a")));

        assertEquals(delete1.hashCode(), delete2.hashCode());
        assertNotEquals(delete1.hashCode(), delete3.hashCode());
        assertNotEquals(delete2.hashCode(), delete3.hashCode());
        assertNotEquals(delete3.hashCode(), delete4.hashCode());

    }

    @Test
    public void testEquals() {

        DeleteInstruction delete1 = new DeleteInstruction(Arrays.asList(mkTerm("a"), mkTerm("b")));
        DeleteInstruction delete2 = new DeleteInstruction(Arrays.asList(mkTerm("a"), mkTerm("b")));
        DeleteInstruction delete3 = new DeleteInstruction(Arrays.asList(mkTerm("a"), mkTerm("c")));
        DeleteInstruction delete4 = new DeleteInstruction(Arrays.asList(mkTerm("c"), mkTerm("a")));

        assertEquals(delete1, delete2);
        assertEquals(delete1.hashCode(), delete2.hashCode());
        assertNotEquals(delete1, delete3);
        assertNotEquals(delete2, delete3);
        assertNotEquals(delete3, delete4);
        assertNotEquals(delete1, null);
        assertNotEquals(delete1, new Object());

    }
}
