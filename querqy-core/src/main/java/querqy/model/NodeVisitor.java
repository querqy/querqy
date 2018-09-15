/**
 * 
 */
package querqy.model;

/**
 * @author René Kriegler, @renekrie
 *
 */
public interface NodeVisitor<T> {

   T visit(MatchAllQuery query);

   T visit(Query query);

   T visit(DisjunctionMaxQuery disjunctionMaxQuery);

   T visit(BooleanQuery booleanQuery);

   T visit(Term term);

   T visit(RawQuery rawQuery);

}
