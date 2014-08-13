/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;

import org.apache.lucene.search.Query;

/**
 * @author rene
 *
 */
public interface LuceneQueryFactory<T extends Query> {
    
    T createQuery(int dfToSet, IndexStats indexStats) throws IOException;
    int getMaxDocFreqInSubtree(IndexStats indexStats);

}
