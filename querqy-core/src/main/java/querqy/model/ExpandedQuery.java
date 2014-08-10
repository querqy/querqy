/**
 * 
 */
package querqy.model;

import java.util.Collection;
import java.util.LinkedList;

/**
 *
 * 
 * @author René Kriegler, @renekrie
 * 
 * Note: this class does not synchronize access to filterQueries.
 *
 */
public class ExpandedQuery {

	private Query userQuery;
	protected Collection<QuerqyQuery<?>> filterQueries;
	
	public ExpandedQuery(Query userQuery) {
		setUserQuery(userQuery);
	}
	
	public Query getUserQuery() {
		return userQuery;
	}
	
	public final void setUserQuery(Query userQuery) {
		if (userQuery == null) {
			throw new IllegalArgumentException("userQuery required");
		}
		this.userQuery = userQuery;
	}
	
	
	public Collection<QuerqyQuery<?>> getFilterQueries() {
		return filterQueries;
	}
	
	public void addFilterQuery(QuerqyQuery<?> filterQuery) {
		if (filterQueries == null) {
			filterQueries = new LinkedList<>();
		}
		filterQueries.add(filterQuery);
	}
	

}
