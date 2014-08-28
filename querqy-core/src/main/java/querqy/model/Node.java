/**
 * 
 */
package querqy.model;

/**
 * @author René Kriegler, @renekrie
 *
 */
public interface Node {

   <T> T accept(NodeVisitor<T> visitor);

   boolean isGenerated();

   Node getParent();

}
