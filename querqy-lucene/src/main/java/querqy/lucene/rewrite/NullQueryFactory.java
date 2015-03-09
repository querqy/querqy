/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;


/**
 * @author René Kriegler, @renekrie
 *
 */
public class NullQueryFactory implements LuceneQueryFactory<Query> {

    @Override
    public Query createQuery(DocumentFrequencyCorrection dfc, boolean isBelowDMQ)
            throws IOException {
        return new BooleanQuery();
    }

    @Override
    public void collectMaxDocFreqInSubtree(DocumentFrequencyCorrection dfc) {
    }

}
