package querqy.lucene.rewrite;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Test;

public class LuceneTermQueryBuilderTest extends LuceneTestCase {

    @Test
    public void testThatThereIsNoDocumentFrequencyCorrection() {
        assertFalse(new LuceneTermQueryBuilder().getDocumentFrequencyCorrection().isPresent());
    }

    @Test
    public void testThatQueryUsesTermButNoFieldBoost() throws Exception {

        Analyzer analyzer = new StandardAnalyzer();

        Directory directory = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setSimilarity(new ClassicSimilarity());
        IndexWriter indexWriter = new IndexWriter(directory, config);


        TestUtil.addNumDocsWithTextField("f1", "v1 v1", indexWriter, 4);
        TestUtil.addNumDocsWithTextField("f1", "v2", indexWriter, 1);

        indexWriter.close();

        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        indexSearcher.setSimilarity(new ClassicSimilarity());


        final TermQuery termQuery = new LuceneTermQueryBuilder()
                .createTermQuery(new Term("f1", "v1"), new ConstantFieldBoost(3f));
        final Term term = termQuery.getTerm();
        assertEquals("f1", term.field());
        assertEquals("v1", term.text());

        TopDocs topDocs = indexSearcher.search(termQuery, 10);

        final Weight weight = termQuery.createWeight(indexSearcher, ScoreMode.COMPLETE, 4.5f);
        final Explanation explain = weight.explain(indexReader.getContext().leaves().get(0), topDocs.scoreDocs[0].doc);

        String explainText = explain.toString();

        assertTrue(explainText.contains("4.5 = boost")); // 4.5 (query) but ignore field boost
        assertTrue(explainText.contains("4 = docFreq")); // 4 * v1
        assertTrue(explainText.contains("2.0 = freq")); // 2 * v1 in field
    }
}
