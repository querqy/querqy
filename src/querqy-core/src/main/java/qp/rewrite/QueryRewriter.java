/**
 * 
 */
package qp.rewrite;

import querqy.model.Query;

/**
 * @author rene
 *
 */
public interface QueryRewriter {
	
	Query rewrite(Query query);
	
}
