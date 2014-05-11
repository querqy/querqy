/**
 * 
 */
package qp.model;

/**
 * @author rene
 *
 */
public interface Clause extends Node {

	SubQuery<?> getParentQuery();
	//void setParentQuery(SubQuery<?> clause);
	
}
