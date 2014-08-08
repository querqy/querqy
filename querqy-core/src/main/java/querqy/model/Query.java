/**
 * 
 */
package querqy.model;


/**
 * @author René Kriegler, @renekrie
 *
 */
public class Query extends BooleanQuery implements QuerqyQuery {
	
	public Query() {
		super(null, Occur.SHOULD, false);
	}
	
	
}
