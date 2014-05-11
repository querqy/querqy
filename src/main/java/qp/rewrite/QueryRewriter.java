/**
 * 
 */
package qp.rewrite;

import qp.model.Query;

/**
 * @author rene
 *
 */
public interface QueryRewriter {
	
	Query rewrite(Query query);
	
}
