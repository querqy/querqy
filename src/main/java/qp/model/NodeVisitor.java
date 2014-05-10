/**
 * 
 */
package qp.model;

/**
 * @author rene
 *
 */
public interface NodeVisitor<T> {
	
	public T visit(Query query);
	public T visit(TermQuery termQuery);
	public T visit(BooleanQuery booleanQuery);

}
