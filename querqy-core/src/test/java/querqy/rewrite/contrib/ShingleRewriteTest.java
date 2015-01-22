package querqy.rewrite.contrib;

import org.junit.Test;

import querqy.model.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static querqy.QuerqyMatchers.*;

/**
 * Test for ShingleRewriter.
 */
public class ShingleRewriteTest {

    @Test
    public void testShinglingForTwoTokens() {
        Query query = new Query();
        addTerm(query, "cde");
        addTerm(query, "ajk");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ShingleRewriter rewriter = new ShingleRewriter();
        rewriter.rewrite(expandedQuery);

        assertThat(expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("cde"),
                                bq(
                                        dmq(must(), term("cdeajk")),
                                        dmq(mustNot(), term("cde"))
                                )
                        ),
                        dmq(term("ajk"),
                                bq(
                                        dmq(must(), term("cdeajk")),
                                        dmq(mustNot(), term("ajk"))
                                )
                        )
                )
        );
    }
    
    @Test
    public void testThatShinglingDoesNotTriggerExceptionOnSingleTerm() throws Exception {
        Query query = new Query();
        addTerm(query, "t1");
        
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ShingleRewriter rewriter = new ShingleRewriter();
        rewriter.rewrite(expandedQuery);

        assertThat(expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("t1")
                        )
                )
        );
    }

    @Test
    public void testShinglingForTwoTokensWithSameField() {
        Query query = new Query();
        addTerm(query, "f1", "cde");
        addTerm(query, "f1", "ajk");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ShingleRewriter rewriter = new ShingleRewriter();
        rewriter.rewrite(expandedQuery);

        assertThat(expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("f1", "cde"),
                                bq(
                                        dmq(must(), term("f1", "cdeajk")),
                                        dmq(mustNot(), term("f1", "cde"))
                                )
                        ),
                        dmq(term("f1", "ajk"),
                                bq(
                                        dmq(must(), term("f1", "cdeajk")),
                                        dmq(mustNot(), term("f1", "ajk"))
                                )
                        )
                )
        );
    }

    @Test
    public void testShinglingForTwoTokensWithDifferentFieldsDontShingle() {
        Query query = new Query();
        addTerm(query, "f1", "cde");
        addTerm(query, "f2", "ajk");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ShingleRewriter rewriter = new ShingleRewriter();
        rewriter.rewrite(expandedQuery);

        assertThat(expandedQuery.getUserQuery(), bq(dmq(term("cde")), dmq(term("ajk"))));
    }
    
    @Test
    public void testShinglingForTwoTokensWithOnFieldNameNullDontShingle() {
        Query query = new Query();
        addTerm(query, "f1", "cde");
        addTerm(query, "ajk");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ShingleRewriter rewriter = new ShingleRewriter();
        rewriter.rewrite(expandedQuery);

        assertThat(expandedQuery.getUserQuery(), bq(dmq(term("f1", "cde")), dmq(term("ajk"))));
    }

    @Test
    public void testShinglingForThreeTokens() {
        Query query = new Query();
        addTerm(query, "cde");
        addTerm(query, "ajk");
        addTerm(query, "xyz");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ShingleRewriter rewriter = new ShingleRewriter();
        rewriter.rewrite(expandedQuery);

        assertThat(expandedQuery.getUserQuery(),
                bq(
                        dmq(term("cde"),
                                bq(
                                        dmq(must(), term("cdeajk")),
                                        dmq(mustNot(), term("cde"))
                                )
                        ),
                        dmq(term("ajk"),
                                bq(
                                        dmq(must(), term("cdeajk")),
                                        dmq(mustNot(), term("ajk"))
                                ),
                                bq(
                                        dmq(must(), term("ajkxyz")),
                                        dmq(mustNot(), term("ajk"))
                                )
                        ),
                        dmq(term("xyz"),
                                bq(
                                        dmq(must(), term("ajkxyz")),
                                        dmq(mustNot(), term("xyz"))
                                )
                        )
                )
        );
    }

    @Test
    public void testShinglingForThreeTokensWithMixedFields() {
        Query query = new Query();
        addTerm(query, "f1", "cde");
        addTerm(query, "f1", "ajk");
        addTerm(query, "f2", "xyz");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ShingleRewriter rewriter = new ShingleRewriter();
        rewriter.rewrite(expandedQuery);

        assertThat(expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("f1", "cde"),
                                bq(
                                        dmq(must(), term("f1", "cdeajk")),
                                        dmq(mustNot(), term("f1", "cde"))
                                )
                        ),
                        dmq(term("f1", "ajk"),
                                bq(
                                        dmq(must(), term("f1", "cdeajk")),
                                        dmq(mustNot(), term("f1", "ajk"))
                                )
                        ),
                        dmq(term("f2", "xyz"))
                )
        );
    }

    private void addTerm(Query query, String value) {
        addTerm(query, null, value);
    }

    private void addTerm(Query query, String field, String value) {
        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, false);
        query.addClause(dmq);
        Term term = new Term(dmq, field, value);
        dmq.addClause(term);
    }

}
