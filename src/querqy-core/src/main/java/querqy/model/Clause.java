/**
 * 
 */
package querqy.model;

/**
 * @author rene
 *
 */
public interface Clause<P extends SubQuery<?>> extends Node {

	P getParentQuery();
	
}
