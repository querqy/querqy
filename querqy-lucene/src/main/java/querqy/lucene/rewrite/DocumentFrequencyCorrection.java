/**
 * 
 */
package querqy.lucene.rewrite;

import org.apache.lucene.index.Term;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class DocumentFrequencyCorrection {
	
	enum Status {USER_QUERY, OTHER_QUERY}
	
	Status status;
	final IndexStats indexStats;
	
	int maxInClause = -1;
	private int maxInUserQuery = -1;
	
	
	public DocumentFrequencyCorrection(IndexStats indexStats) {
		status = Status.USER_QUERY;
		this.indexStats = indexStats;
	}
	
	void newClause() {
		 if (status == Status.USER_QUERY) {
			 maxInUserQuery = Math.max(maxInClause, maxInUserQuery);
		 }
		 maxInClause = -1;
	}
	
	public void finishedUserQuery() {
		status = Status.OTHER_QUERY;
		maxInUserQuery = Math.max(maxInClause, maxInUserQuery);
	}
	
	int collect(Term term) {
		int df = indexStats.df(term);
		maxInClause = Math.max(df,maxInClause);
		return df;
	}
	
	int getDocumentFrequencyToSet() {
		return  (status == Status.USER_QUERY || maxInUserQuery < 1) ? maxInClause : maxInClause + maxInUserQuery - 1;
	}

}
