package querqy.rewrite.contrib;

import org.junit.Test;

import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.model.Term;

import static org.hamcrest.MatcherAssert.assertThat;
import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.term;

public class NumberQueryRewriterTest {

    @Test
    public void testNumberConcatenationForTwoTokens() {
        Query query = new Query();
        addTerm(query, "123");
        addTerm(query, "456");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        NumberQueryRewriter rewriter = new NumberQueryRewriter(false, 3);
        rewriter.rewrite(expandedQuery, null);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("123"),
                                term("123456")
                        ),
                        dmq(
                                term("456"),
                                term("123456")
                        )
                )
        );
    }

    @Test
    public void testNumberConcatenationForMultipleTokens() {
        Query query = new Query();
        addTerm(query, "a");
        addTerm(query, "123");
        addTerm(query, "456");
        addTerm(query, "789");
        addTerm(query, "b");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        NumberQueryRewriter rewriter = new NumberQueryRewriter(false, 3);
        rewriter.rewrite(expandedQuery, null);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("a")
                        ),
                        dmq(
                                term("123"),
                                term("123456789")
                        ),
                        dmq(
                                term("456"),
                                term("123456789")
                        ),
                        dmq(
                                term("789"),
                                term("123456789")
                        ),
                        dmq(
                                term("b")
                        )
                )
        );
    }

    @Test
    public void testSingleNumericTokenIsPreserved() {
        Query query = new Query();
        addTerm(query, "1");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        NumberQueryRewriter rewriter = new NumberQueryRewriter(false, 3);
        rewriter.rewrite(expandedQuery, null);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("1")
                        )
                )
        );
    }

    @Test
    public void testNumberConcatenationWithinSameField() {
        Query query = new Query();
        addTerm(query, "f1", "123");
        addTerm(query, "f1", "456");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        NumberQueryRewriter rewriter = new NumberQueryRewriter(false, 3);
        rewriter.rewrite(expandedQuery, null);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("f1", "123"),
                                term("f1", "123456")
                        ),
                        dmq(
                                term("f1", "456"),
                                term("f1", "123456")
                        )
                )
        );
    }

    @Test
    public void testNoNumberConcatenationForDifferentFields() {
        Query query = new Query();
        addTerm(query, "f1", "123");
        addTerm(query, "f2", "456");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        NumberQueryRewriter rewriter = new NumberQueryRewriter(false, 3);
        rewriter.rewrite(expandedQuery, null);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("f1", "123")
                        ),
                        dmq(
                                term("f2", "456")
                        )
                )
        );
    }

    @Test
    public void testNoNumberConcatenationForFieldAndAllFieldsQuery() {
        Query query = new Query();
        addTerm(query, "123");
        addTerm(query, "f", "456");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        NumberQueryRewriter rewriter = new NumberQueryRewriter(false, 3);
        rewriter.rewrite(expandedQuery, null);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term(null, "123")
                        ),
                        dmq(
                                term("f", "456")
                        )
                )
        );
    }

    @Test
    public void testNoNumberConcatenationForGeneratedTerms() {
        Query query = new Query();
        addTerm(query, "123");
        addTerm(query, "456", true);
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        NumberQueryRewriter rewriter = new NumberQueryRewriter(false, 3);
        rewriter.rewrite(expandedQuery, null);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term(null, "123")
                        ),
                        dmq(
                                term(null, "456")
                        )
                )
        );
    }

    @Test
    public void testNumberConcatenationForGeneratedTermsIfConfigured() {
        Query query = new Query();
        addTerm(query, "123");
        addTerm(query, "456", true);
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        NumberQueryRewriter rewriter = new NumberQueryRewriter(true, 3);
        rewriter.rewrite(expandedQuery, null);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term(null, "123"),
                                term(null, "123456")
                        ),
                        dmq(
                                term(null, "456"),
                                term(null, "123456")
                        )
                )
        );
    }

    private void addTerm(Query query, String value) {
        addTerm(query, null, value);
    }

    private void addTerm(Query query, String field, String value) {
        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, true);
        query.addClause(dmq);
        Term term = new Term(dmq, field, value);
        dmq.addClause(term);
    }

    private void addTerm(Query query, String value, boolean isGenerated) {
        addTerm(query, null, value, isGenerated);
    }

    private void addTerm(Query query, String field, String value, boolean isGenerated) {
        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, true);
        query.addClause(dmq);
        Term term = new Term(dmq, field, value, isGenerated);
        dmq.addClause(term);
    }


}
