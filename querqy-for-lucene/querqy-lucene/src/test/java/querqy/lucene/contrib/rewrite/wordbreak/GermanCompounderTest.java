package querqy.lucene.contrib.rewrite.wordbreak;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Test;
import querqy.model.Term;

import java.io.IOException;
import java.util.List;


public class GermanCompounderTest extends LuceneTestCase {

    @Test
    public void testWithEmptyIndex() throws IOException {
        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final String field = "f1";
            final MorphologicalCompounder compounder = new MorphologicalCompounder(new MorphologyProvider().get("GERMAN").get(), field, true, 1);
            final querqy.model.Term left = new querqy.model.Term(null, field, "left", false);
            final querqy.model.Term right = new querqy.model.Term(null, field, "left", false);
            final List<LuceneCompounder.CompoundTerm> sequences = compounder.combine(new Term[]{left, right}, indexReader, false);
            assertNotNull(sequences);
            assertTrue(sequences.isEmpty());

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }
}
