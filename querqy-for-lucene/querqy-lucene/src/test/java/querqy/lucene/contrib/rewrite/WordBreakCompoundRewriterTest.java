package querqy.lucene.contrib.rewrite;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;
import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.must;
import static querqy.QuerqyMatchers.term;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.spell.SuggestMode;
import org.apache.lucene.search.spell.SuggestWord;
import org.apache.lucene.search.spell.WordBreakSpellChecker;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Query;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class WordBreakCompoundRewriterTest {

    @Mock
    WordBreakSpellChecker wordBreakSpellChecker;

    @Mock
    IndexReader indexReader;

    @Test
    public void testDecompoundSingleTokenIntoOneTwoTokenAlternative() throws IOException {

        SuggestWord word1 = new SuggestWord();
        word1.string = "w1";

        SuggestWord word2 = new SuggestWord();
        word2.string = "w2";

        when(wordBreakSpellChecker.suggestWordBreaks( any(), anyInt(), any(), any(), any()))
                .thenReturn(new SuggestWord[][] {new SuggestWord[] {word1, word2}});

        WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(wordBreakSpellChecker, indexReader, "field1");
        Query query = new Query();
        addTerm(query, "input", false);


        ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery);

        assertThat((Query) rewritten.getUserQuery(),
                bq(
                        dmq(
                                term("input", false),
                                bq(
                                        dmq(must(), term("w1", true)),
                                        dmq(must(), term("w2", true))
                                )

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
        querqy.model.Term term = new querqy.model.Term(dmq, field, value);
        dmq.addClause(term);
    }

    private void addTerm(Query query, String value, boolean isGenerated) {
        addTerm(query, null, value, isGenerated);
    }

    private void addTerm(Query query, String field, String value, boolean isGenerated) {
        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, true);
        query.addClause(dmq);
        querqy.model.Term term = new querqy.model.Term(dmq, field, value, isGenerated);
        dmq.addClause(term);
    }
}
