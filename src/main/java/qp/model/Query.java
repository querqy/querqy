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
		super(Operator.NONE, Occur.SHOULD);
	}
	
	
}
