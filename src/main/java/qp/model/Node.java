/**
 * 
 */
package qp.model;


/**
 * @author rene
 *
 */
public interface Node {
	
	<T> T accept(NodeVisitor<T> visitor);

}
