package querqy.lucene.rewrite;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.LuceneTestCase;

import java.io.IOException;

/**
 * Created by rene on 14/09/2016.
 */
public class TestUtil {

    public static final String LUCENE_CODEC = System.getProperty("tests.codec");

    public static void addNumDocsWithStringField(String fieldname, String value, IndexWriter indexWriter, int num) throws IOException {
        for (int i = 0; i < num; i++) {
            Document doc = new Document();
            doc.add(LuceneTestCase.newStringField(fieldname, value, Field.Store.YES));
            indexWriter.addDocument(doc);
        }
    }

    public static void addNumDocsWithTextField(String fieldname, String value, IndexWriter indexWriter, int num) throws IOException {
        for (int i = 0; i < num; i++) {
            Document doc = new Document();
            doc.add(new TextField(fieldname, value, Field.Store.YES));
            indexWriter.addDocument(doc);
        }
    }

    public static void addNumDocsWithStringField(String fieldname, String value, RandomIndexWriter indexWriter, int num) throws IOException {
        for (int i = 0; i < num; i++) {
            Document doc = new Document();
            doc.add(LuceneTestCase.newStringField(fieldname, value, Field.Store.YES));
            indexWriter.addDocument(doc);
        }
    }

    public static void addNumDocsWithTextField(String fieldname, String value, RandomIndexWriter indexWriter, int num) throws IOException {
        for (int i = 0; i < num; i++) {
            Document doc = new Document();
            doc.add(LuceneTestCase.newTextField(fieldname, value, Field.Store.YES));
            indexWriter.addDocument(doc);
        }
    }

    public static Term newTerm(String field, String value, DocumentFrequencyCorrection dfc) {
        Term term = new Term(field, value);
        dfc.prepareTerm(term);
        return term;
    }
}
