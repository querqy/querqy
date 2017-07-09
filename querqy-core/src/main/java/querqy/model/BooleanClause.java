/**
 * 
 */
package querqy.model;

import querqy.model.Clause.Occur;

/**
 * @author René Kriegler, @renekrie
 *
 */
public interface BooleanClause extends Node {

    BooleanClause clone(BooleanQuery newParent);
    BooleanClause clone(BooleanQuery newParent, boolean generated);
    BooleanClause clone(BooleanQuery newParent, Occur occur);
    BooleanClause clone(BooleanQuery newParent, Occur occur, boolean generated);
    Occur getOccur();

}
