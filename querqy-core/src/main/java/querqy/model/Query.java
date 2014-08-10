/**
 * 
 */
package querqy.model;


/**
 * @author René Kriegler, @renekrie
 *
 */
public class Query extends BooleanQuery implements QuerqyQuery<BooleanParent> {
	
	public Query() {
		super(null, Occur.SHOULD, false);
	}
	
	
}
