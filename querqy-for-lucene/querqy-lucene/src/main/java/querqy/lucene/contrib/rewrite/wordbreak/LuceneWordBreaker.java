package querqy.lucene.contrib.rewrite.wordbreak;

import org.apache.lucene.index.IndexReader;

import java.io.IOException;
import java.util.List;

/**
 * @author renekrie
 */
public interface LuceneWordBreaker {

    List<CharSequence[]> breakWord(CharSequence word, IndexReader indexReader, int maxDecompoundExpansions,
                                 boolean verifyCollation) throws IOException;
}
