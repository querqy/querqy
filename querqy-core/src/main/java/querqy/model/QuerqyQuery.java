/**
 * 
 */
package querqy.model;

/**
 * @author René Kriegler, @renekrie
 *
 */
public interface QuerqyQuery<P extends Node> extends CloneableNode<P> {

	QuerqyQuery<P> clone(P newParent);
}
