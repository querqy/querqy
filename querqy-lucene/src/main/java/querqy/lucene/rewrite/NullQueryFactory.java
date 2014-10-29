/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;


/**
 * @author René Kriegler, @renekrie
 *
 */
public class NullQueryFactory implements LuceneQueryFactory<Query> {

    @Override
    public Query createQuery(DocumentFrequencyCorrection dfc, boolean isBelowDMQ)
            throws IOException {
        BooleanQuery bq = new BooleanQuery(true);
        bq.add(new MatchAllDocsQuery(), Occur.MUST_NOT);
        return bq;
    }

    @Override
    public void collectMaxDocFreqInSubtree(DocumentFrequencyCorrection dfc) {
    }

}
