/**
 * 
 */
package querqy.rewrite.lucene;

import org.apache.lucene.search.Query;

/**
 * @author rene
 *
 */
public interface LuceneQueryFactory<T extends Query> {
    
    T createQuery(int dfToSet, IndexStats indexStats);
    int getMaxDocFreqInSubtree(IndexStats indexStats);

}
