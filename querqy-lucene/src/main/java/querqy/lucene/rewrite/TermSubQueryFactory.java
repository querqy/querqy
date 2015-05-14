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
public class TermSubQueryFactory implements LuceneQueryFactory<Query> {
    
    final LuceneQueryFactory<?> root;
    final Float boost;
    
    public TermSubQueryFactory(LuceneQueryFactory<?> root, Float boost) {
        this.root = root;
        this.boost = boost;
    }

    @Override
    public Query createQuery(Float boostFactor, float dmqTieBreakerMultiplier, DocumentFrequencyCorrection dfc,
            boolean isBelowDMQ) throws IOException {
        
        return root.createQuery(
                boostFactor != null ? boostFactor : boost, 
                dmqTieBreakerMultiplier,         
                dfc, isBelowDMQ);
    }

}
