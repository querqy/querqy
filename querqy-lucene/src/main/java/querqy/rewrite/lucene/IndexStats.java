/**
 * 
 */
package querqy.rewrite.lucene;

import org.apache.lucene.index.Term;

/**
 * @author rene
 *
 */
public interface IndexStats {
    
    int df(Term term);

}
