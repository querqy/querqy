/**
 * 
 */
package querqy.rewrite;

import querqy.model.ExpandedQuery;

/**
 * A query rewriter.
 * 
 * @author rene
 *
 */
public interface QueryRewriter {

   ExpandedQuery rewrite(ExpandedQuery query);

}
