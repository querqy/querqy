/**
 * 
 */
package querqy.model;

/**
 * @author René Kriegler, @renekrie
 *
 */
public interface QuerqyQuery<P extends Node> extends CloneableNode<P> {

	@Override
   QuerqyQuery<P> clone(P newParent);
}
