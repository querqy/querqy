package querqy.rewrite.replace;

import org.junit.Test;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.model.Term;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.term;

public class RegexReplaceRewriterTest {

    @Test
    public void testLiteralsOnly() throws IOException {
        RegexReplaceRewriterFactory factory = factory("""
                abc => def
                yxz => mno # comment
                """);

        assertThat((Query) factory
                        .createRewriter(null, null)
                        .rewrite(query("pref abc"), new EmptySearchEngineRequestAdapter()).getExpandedQuery()
                        .getUserQuery(),
                bq(
                        dmq(term("pref")),
                        dmq(term("def"))
                )
        );

        assertThat((Query) factory
                        .createRewriter(null, null)
                                .rewrite(query("abc"), new EmptySearchEngineRequestAdapter()).getExpandedQuery()
                        .getUserQuery(),
                bq(
                        dmq(term("def"))
                )
        );

        assertThat((Query) factory
                        .createRewriter(null, null)
                        .rewrite(query("yxz"), new EmptySearchEngineRequestAdapter()).getExpandedQuery()
                        .getUserQuery(),
                bq(
                        dmq(term("mno"))
                )
        );

        assertThat((Query) factory
                        .createRewriter(null, null)
                        .rewrite(query("abc klm"), new EmptySearchEngineRequestAdapter()).getExpandedQuery()
                        .getUserQuery(),
                bq(
                        dmq(term("def")),
                        dmq(term("klm"))
                )
        );

        assertThat((Query) factory
                        .createRewriter(null, null)
                        .rewrite(query("abc yxz"), new EmptySearchEngineRequestAdapter()).getExpandedQuery()
                        .getUserQuery(),
                bq(
                        dmq(term("def")),
                        dmq(term("mno"))
                )
        );

    }

    @Test
    public void testLiteralsWithPlaceHolder() throws IOException {
        RegexReplaceRewriterFactory factory = factory("""
                (abc) => def x${1}
                (yxz) => ${1} mno # comment
                lmn => k
                """);

        assertThat((Query) factory
                        .createRewriter(null, null)
                        .rewrite(query("abc"), new EmptySearchEngineRequestAdapter()).getExpandedQuery()
                        .getUserQuery(),
                bq(
                        dmq(term("def")),
                        dmq(term("xabc"))
                )
        );

        assertThat((Query) factory
                        .createRewriter(null, null)
                        .rewrite(query("abc yxz"), new EmptySearchEngineRequestAdapter()).getExpandedQuery()
                        .getUserQuery(),
                bq(
                        dmq(term("def")),
                        dmq(term("xabc")),
                        dmq(term("yxz")),
                        dmq(term("mno"))

                )
        );

        assertThat((Query) factory
                        .createRewriter(null, null)
                        .rewrite(query("pref lmn abc 1234 567"), new EmptySearchEngineRequestAdapter()).getExpandedQuery()
                        .getUserQuery(),
                bq(
                        dmq(term("pref")),
                        dmq(term("k")),
                        dmq(term("def")),
                        dmq(term("xabc")),
                        dmq(term("1234")),
                        dmq(term("567"))

                )
        );
    }

    @Test
    public void testRuleOrdering() throws IOException {
        RegexReplaceRewriterFactory factory = factory("""
                a\\dc => first
                a1c => second
                d1f => third
                d\\df => forth
                """);

        assertThat((Query) factory
                        .createRewriter(null, null)
                        .rewrite(query("a1c"), new EmptySearchEngineRequestAdapter()).getExpandedQuery()
                        .getUserQuery(),
                bq(
                        dmq(term("second"))
                )
        );

        assertThat((Query) factory
                        .createRewriter(null, null)
                        .rewrite(query("d1f"), new EmptySearchEngineRequestAdapter()).getExpandedQuery()
                        .getUserQuery(),
                bq(
                        dmq(term("forth"))
                )
        );
    }

    static RegexReplaceRewriterFactory factory(final String rules) throws IOException {
        return factory(rules, true);
    }

    static RegexReplaceRewriterFactory factory(final String rules, final boolean ignoreCase) throws IOException {
        InputStream stream = new ByteArrayInputStream(rules.getBytes
                (StandardCharsets.UTF_8));
        return new RegexReplaceRewriterFactory("id1", new InputStreamReader(stream), ignoreCase);
    }

    static ExpandedQuery query(String query) {
        return query(Arrays.asList(query.split(" +")));
    }
    static ExpandedQuery query(List<String> tokens) {
        Query query = new Query();
        tokens.forEach(token -> addTerm(query, token));
        return new ExpandedQuery(query);
    }

    static void addTerm(Query query, String value) {
        addTerm(query, null, value);
    }

    static void addTerm(Query query, String field, String value) {
        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, true);
        query.addClause(dmq);
        Term term = new Term(dmq, field, value);
        dmq.addClause(term);
    }

    static void addTerm(Query query, String value, boolean isGenerated) {
        addTerm(query, null, value, isGenerated);
    }

    static void addTerm(Query query, String field, String value, boolean isGenerated) {
        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, true);
        query.addClause(dmq);
        Term term = new Term(dmq, field, value, isGenerated);
        dmq.addClause(term);
    }
}