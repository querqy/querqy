/**
 * 
 */
package querqy.lucene.rewrite;

import org.apache.lucene.search.Query;

/**
 * @author rene
 *
 */
public abstract class AbstractLuceneQueryFactory<T extends Query> implements LuceneQueryFactory<T> {
    
    public static final float DEFAULT_BOOST_FACTOR = 1f;
    
    protected final Float boost;
    
    
    protected AbstractLuceneQueryFactory(Float boost) {
        this.boost = boost;
    }
    
    
    protected float getBoostFactor(Float requestBoostFactor) {
        return getBoostFactor(requestBoostFactor, DEFAULT_BOOST_FACTOR);
    }
    
    protected float getBoostFactor(Float requestBoostFactor, float defaultBoostFactor) {
        if (requestBoostFactor != null) {
            return requestBoostFactor;
        }
        if (boost != null) {
            return boost;
        }
        return defaultBoostFactor;
    }

}
