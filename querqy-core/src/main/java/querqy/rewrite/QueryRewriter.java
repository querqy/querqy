/**
 * 
 */
package querqy.rewrite;

import querqy.model.ExpandedQuery;

/**
 * @author rene
 *
 */
public interface QueryRewriter {

   ExpandedQuery rewrite(ExpandedQuery query);

}
