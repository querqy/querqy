package querqy.lucene.rewrite;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.LuceneTestCase;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by rene on 14/09/2016.
 */
public class TestUtil {

    public static final String LUCENE_CODEC = System.getProperty("tests.codec");

    public static void addNumDocsWithStringField(final String fieldname, final String value,
                                                 final IndexWriter indexWriter, final int num) throws IOException {
        indexWriter.addDocuments(IntStream.range(0, num).mapToObj(i -> {

            final Document doc = new Document();
            doc.add(LuceneTestCase.newStringField(fieldname, value, Field.Store.YES));
            return doc;

        }).collect(Collectors.toList()));
    }

    public static void addNumDocsWithTextField(final String fieldname, final String value,
                                               final IndexWriter indexWriter, int num) throws IOException {
        indexWriter.addDocuments(IntStream.range(0, num).mapToObj(i -> {

            final Document doc = new Document();
            doc.add(LuceneTestCase.newTextField(fieldname, value, Field.Store.YES));
            return doc;

        }).collect(Collectors.toList()));
    }

    public static void addNumDocsWithStringField(final String fieldname, final String value,
                                                 final RandomIndexWriter indexWriter, final int num) throws IOException {

        indexWriter.addDocuments(IntStream.range(0, num).mapToObj(i -> {

            final Document doc = new Document();
            doc.add(LuceneTestCase.newStringField(fieldname, value, Field.Store.YES));
            return doc;

        }).collect(Collectors.toList()));

    }

    public static void addNumDocsWithTextField(final String fieldname, final String value,
                                               final RandomIndexWriter indexWriter, final int num) throws IOException {

        indexWriter.addDocuments(IntStream.range(0, num).mapToObj(i -> {

            final Document doc = new Document();
            doc.add(LuceneTestCase.newTextField(fieldname, value, Field.Store.YES));
            return doc;

        }).collect(Collectors.toList()));

    }

    public static Term newTerm(final String field, final String value, final DocumentFrequencyCorrection dfc) {
        Term term = new Term(field, value);
        dfc.prepareTerm(term);
        return term;
    }
}
