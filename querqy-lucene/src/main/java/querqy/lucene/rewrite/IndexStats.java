/**
 * 
 */
package querqy.lucene.rewrite;

import org.apache.lucene.index.Term;

/**
 * @author rene
 *
 */
public interface IndexStats {
    
    int df(Term term);

}
