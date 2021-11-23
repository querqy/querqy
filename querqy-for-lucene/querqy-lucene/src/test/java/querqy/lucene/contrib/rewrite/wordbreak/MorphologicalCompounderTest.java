package querqy.lucene.contrib.rewrite.wordbreak;


import org.apache.lucene.index.IndexReader;
import org.hamcrest.Matchers;
import org.junit.Test;
import querqy.model.*;
import querqy.trie.TrieMap;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static querqy.QuerqyMatchers.*;
import static querqy.lucene.contrib.rewrite.wordbreak.LuceneCompounder.*;

public class MorphologicalCompounderTest {
    private final LuceneWordBreaker noOp = (word, indexReader, maxDecompoundExpansions, verifyCollation) -> Collections.emptyList();
    private final Morphology noopMorphology = new Morphology() {
        @Override
        public Compound[] suggestCompounds(CharSequence left, CharSequence right) {
            return new Compound[0];
        }

        @Override
        public List<WordBreak> suggestWordBreaks(CharSequence word, int minBreakLength) {
            return Collections.emptyList();
        }
    };
    private final MorphologicalCompounder compounder = new MorphologicalCompounder(noopMorphology, "f1", true, 1);

    @Test
    public void emptyCollectionWhenLessThan2Terms() {
        final List<CompoundTerm> combine = compounder.combine(new Term[]{}, null, false);
        assertThat(combine, Matchers.hasSize(0));
    }

    @Test
    public void simpleMorphologicalWordBreakCompoundRewriter() throws Exception {

        Morphology morphology = new Morphology() {
            @Override
            public Compound[] suggestCompounds(CharSequence left, CharSequence right) {
                return new Compound[0];
            }

            @Override
            public List<WordBreak> suggestWordBreaks(CharSequence word, int minBreakLength) {
                return null;
            }
        };
        final MorphologicalCompounder compounder = new MorphologicalCompounder(morphology, "field1", false, 1);

        // don't de-compound

        final IndexReader indexReader = mock(IndexReader.class);
        when(indexReader.docFreq(any())).thenReturn(1);


        final WordBreakCompoundRewriter rewriter = new WordBreakCompoundRewriter(
                noOp,
                compounder,
                indexReader, false, false, new TrieMap<>(), 5, false, new TrieMap<>()
        );

        final Query query = new QueryBuilder().withQuery().addTerm("w1", false).addTerm("w2", false).build();

        final ExpandedQuery expandedQuery = new ExpandedQuery(query);

        final ExpandedQuery rewritten = rewriter.rewrite(expandedQuery);

        assertThat((Query) rewritten.getUserQuery(),
                bq(
                        dmq(
                                term("w1", false),
                                term("w1w2", true)
                        ),
                        dmq(
                                term("w2", false),
                                term("w1w2", true)
                        )
                )
        );
    }

    static class QueryBuilder {

        private Query query;

        public QueryBuilder withQuery() {
            query = new Query();
            return this;
        }

        public QueryBuilder addTerm(final String word, final boolean isGenerated) {
            if (query == null) {
                throw new IllegalStateException("with Query should be invoked first");
            }
            final DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, true);
            query.addClause(dmq);
            final querqy.model.Term term = new querqy.model.Term(dmq, null, word, isGenerated);
            dmq.addClause(term);
            return this;
        }

        public Query build() {
            if (query == null) {
                throw new IllegalStateException("with Query should be invoked first");
            }
            return query;
        }
    }
}
