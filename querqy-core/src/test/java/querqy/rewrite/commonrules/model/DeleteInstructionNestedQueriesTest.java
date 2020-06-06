package querqy.rewrite.commonrules.model;

import org.junit.Test;
import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.model.builder.QueryBuilder;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;

import static org.assertj.core.api.Assertions.assertThat;
import static querqy.model.builder.BooleanQueryBuilder.bool;
import static querqy.model.builder.DisjunctionMaxQueryBuilder.dmq;
import static querqy.model.builder.QueryBuilder.query;
import static querqy.model.builder.TermBuilder.term;

public class DeleteInstructionNestedQueriesTest extends AbstractCommonRulesTest {

    // TODO: redefine assertions not to use string comparisons anymore as soon as the builder lib is enhanced

    @Test
    public void testDeletionOfNestedBooleanQuery() {
        CommonRulesRewriter rewriter = rewriter(
                addRule(input("a", "b"),
                        delete("a", "b"))
        );

        QueryBuilder q =
                query(
                        dmq(
                                term("ab"),
                                bool(
                                        dmq("a"),
                                        dmq("b")
                                )
                        ),
                        dmq("b")
                );

        ExpandedQuery query = new ExpandedQuery(q.build());
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

        assertThat(QueryBuilder.fromQuery(rewritten).toString())
                .isEqualTo(query(dmq("ab"), dmq("b")).toString());
    }

    @Test
    public void testThatSequencesAcrossNestedBooleanQueriesAreNotRemoved() {

        CommonRulesRewriter rewriter = rewriter(
            addRule(input("a", "b"),
                    delete("a", "b"))
        );

        QueryBuilder q =
                query(
                        dmq(
                                term("a"),
                                bool(
                                        dmq("b")
                                )
                        )
                );

        ExpandedQuery query = new ExpandedQuery(q.build());
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

        assertThat(QueryBuilder.fromQuery(rewritten).toString())
                .isEqualTo(q.toString());
    }


    // TODO: move subsequent tests to DeleteInstructionTest as soon as the builder lib is more established

    @Test
    public void testThatDmqContainingTheSameTermMultipleTimesIsFullyRemoved() {
        CommonRulesRewriter rewriter = rewriter(
                addRule(input("a"),
                        delete("a"))
        );

        QueryBuilder q = query(dmq("a", "a", "a"));

        ExpandedQuery query = new ExpandedQuery(q.build());
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

        assertThat(QueryBuilder.fromQuery(rewritten).toString())
                .isEqualTo(query().toString());
    }

    @Test
    public void testThatMultipleDmqsContainingTheSameTermAreFullyRemoved() {
        CommonRulesRewriter rewriter = rewriter(
                addRule(input("a"),
                        delete("a"))
        );

        QueryBuilder q = query("a", "a", "a");

        ExpandedQuery query = new ExpandedQuery(q.build());
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

        assertThat(QueryBuilder.fromQuery(rewritten).toString())
                .isEqualTo(query().toString());
    }


    // TODO: move subsequent tests as soon as the builder lib is more established

    @Test
    public void testThatSynonymsAreAppliedOnDuplicateSequences() {
        CommonRulesRewriter rewriter = rewriter(
                addRule(input("a", "b"),
                        synonym("c"))
        );

        QueryBuilder q = query("a", "b", "a", "b");

        ExpandedQuery query = new ExpandedQuery(q.build());
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

        assertThat(QueryBuilder.fromQuery(rewritten).toString())
                .isEqualTo(query(
                        dmq("a", "c"),
                        dmq("b", "c"),
                        dmq("a", "c"),
                        dmq("b", "c")
                ).toString());
    }
}
