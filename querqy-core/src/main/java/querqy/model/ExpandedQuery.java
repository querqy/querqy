/**
 * 
 */
package querqy.model;

import java.util.Collection;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class ExpandedQuery {

	private Query userQuery;
	protected Collection<QuerqyQuery> filterQueries;
	
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
	
	
	public Collection<QuerqyQuery> getFilterQueries() {
		return filterQueries;
	}
	public void setFilterQueries(Collection<QuerqyQuery> filterQueries) {
		this.filterQueries = filterQueries;
	}
	

}
