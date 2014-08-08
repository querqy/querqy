/**
 * 
 */
package querqy.rewrite;

import java.util.Map;

import querqy.model.ExpandedQuery;

/**
 * @author rene
 *
 */
public interface RewriterFactory {
    
    QueryRewriter createRewriter(ExpandedQuery input, Map<String, ?> context);
    
}
