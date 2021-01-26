package querqy.lucene.contrib.rewrite.wordbreak;

import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.must;
import static querqy.QuerqyMatchers.term;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Test;
import querqy.lucene.contrib.rewrite.wordbreak.ClassicWordBreakCompoundRewriterFactory;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.ExpandedQuery;
import querqy.model.Query;

import java.io.IOException;
import java.util.*;

public class WordBreakCompoundRewriterIndexDistributionTest extends LuceneTestCase {


    @Test
    public void testThatExpansionsAreVerifiedAndSortedByFreqDesc() throws IOException {


        final ExpandedQuery rewritten;
        final Analyzer analyzer = new WhitespaceAnalyzer();
        final Directory directory = newDirectory();
        try {
            final RandomIndexWriter writer = new RandomIndexWriter(random(), directory, analyzer);

            final Document doc1 = new Document();
            doc1.add(new TextField("dict", "w1 w2a", Field.Store.NO));

            final Document doc2_1 = new Document();
            doc2_1.add(new TextField("dict", "w 1w2a", Field.Store.NO));

            final Document doc2_2 = new Document();
            doc2_2.add(new TextField("dict", "w 1w2a", Field.Store.NO));

            final Document doc2_3 = new Document();
            doc2_3.add(new TextField("dict", "w 1w2a", Field.Store.NO));

            final Document doc3_1 = new Document();
            doc3_1.add(new TextField("dict", "w1w 2a", Field.Store.NO));

            final Document doc3_2 = new Document();
            doc3_2.add(new TextField("dict", "w1w 2a", Field.Store.NO));

            writer.addDocuments(Arrays.asList(doc1, doc2_1, doc2_2, doc2_3, doc3_1, doc3_2));

            // most frequent terms but not in collation
            for (int i = 0; i < 10; i++) {

                final Document docX = new Document();
                docX.add(new TextField("dict", "w1w2", Field.Store.NO));

                final Document docY = new Document();
                docY.add(new TextField("dict", "a", Field.Store.NO));

                writer.addDocuments(Arrays.asList(docX, docY));

            }

            writer.commit();
            writer.close();

            final Query query = new Query();
            addTerm(query, "w1w2a", false);
            final ExpandedQuery expandedQuery = new ExpandedQuery(query);

            final IndexReader reader = DirectoryReader.open(directory);

            try {
                rewritten = new ClassicWordBreakCompoundRewriterFactory("WordBreakCompoundRewriter", () -> reader, "dict",
                        false, 1, 2, 1, Collections.emptyList(), false, 2, true, Collections.emptyList())
                        .createRewriter(expandedQuery, new EmptySearchEngineRequestAdapter())
                        .rewrite(expandedQuery);
            } finally {
                reader.close();
            }


        } finally {
            try {
                analyzer.close();
            } catch (final Exception e) {};
            try {
                directory.close();
            } catch (final Exception e) {};

        }

        assertThat((Query) rewritten.getUserQuery(),
                bq(
                        dmq(
                                term("w1w2a", false),
                                bq(
                                        dmq(must(), term("w", true)),
                                        dmq(must(), term("1w2a", true))
                                ),
                                bq(
                                        dmq(must(), term("w1w", true)),
                                        dmq(must(), term("2a", true))
                                )

                        )

                )
        );


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
