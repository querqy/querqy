/**
 * 
 */
package querqy.model;


/**
 * @author rene
 *
 */
public class Query extends BooleanQuery implements QuerqyQuery {
	
	public Query() {
		super(null, Occur.SHOULD, false);
	}
	
	
}
