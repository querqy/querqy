/**
 * 
 */
package querqy.model;

/**
 * @author René Kriegler, @renekrie
 *
 */
public interface BooleanClause extends Node {

   BooleanClause clone(BooleanQuery newParent);
   BooleanClause clone(BooleanQuery newParent, boolean generated);

}
