/**
 * 
 */
package querqy.model;

/**
 * @author René Kriegler, @renekrie
 *
 */
public interface NodeVisitor<T> {
	
	public T visit(Query query);
	public T visit(DisjunctionMaxQuery disjunctionMaxQuery);
	public T visit(BooleanQuery booleanQuery);
	public T visit(Term term);
	

}
