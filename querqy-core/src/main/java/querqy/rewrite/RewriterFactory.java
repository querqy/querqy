/**
 * 
 */
package querqy.rewrite;

import java.util.Map;

import querqy.model.Query;

/**
 * @author rene
 *
 */
public interface RewriterFactory {
    
    QueryRewriter createRewriter(Query input, Map<String, ?> context);
    
}
