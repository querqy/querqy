/**
 * 
 */
package querqy.model;

/**
 * @author René Kriegler, @renekrie
 *
 */
public interface BooleanParent extends Node {

    void removeClauseAndTraverseTree(BooleanQuery booleanQuery);

}
