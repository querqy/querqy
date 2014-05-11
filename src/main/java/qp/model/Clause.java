/**
 * 
 */
package qp.model;

/**
 * @author rene
 *
 */
public interface Clause extends Node {

	SubQuery<?> getQuery();
	void setQuery(SubQuery<?> clause);
	
}
