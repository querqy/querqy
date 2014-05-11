/**
 * 
 */
package qp.model;


/**
 * @author rene
 *
 */
public class Query extends BooleanQuery {
	
	public Query() {
		super(null, Operator.NONE, Occur.SHOULD);
	}
	
	
}
