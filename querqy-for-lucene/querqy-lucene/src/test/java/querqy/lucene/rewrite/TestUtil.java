package querqy.lucene.rewrite;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.util.LuceneTestCase;

import java.io.IOException;

/**
 * Created by rene on 14/09/2016.
 */
public class TestUtil {

    public static void addNumDocs(String fieldname, String value, RandomIndexWriter indexWriter, int num) throws IOException {
        for (int i = 0; i < num; i++) {
            Document doc = new Document();
            doc.add(LuceneTestCase.newStringField(fieldname, value, Field.Store.YES));
            indexWriter.addDocument(doc);
        }
    }
}
