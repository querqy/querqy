/**
 * 
 */
package querqy.parser;

import querqy.model.Clause.Occur;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Query;
import querqy.model.Term;

/**
 * A simple QuerqyParser that breaks a query string into clauses at whitespace. Clauses
 * are either terms or terms with a boolean operator prefix (&quot;+&quot; &quot;-&quot;). 
 * 
 * @author René Kriegler, @renekrie
 *
 */
public class WhiteSpaceQuerqyParser implements QuerqyParser {

	

	/* (non-Javadoc)
	 * @see querqy.parser.QuerqyParser#parse(java.lang.String)
	 */
	@Override
	public Query parse(String input) {
		String[] tokens = input.split("\\s+");
		Query query = new Query();
		for (String token: tokens) {
			
			Occur occur = Occur.SHOULD;
			
			if (token.length() > 1) {
				if (token.startsWith("+")) {
					occur = Occur.MUST;
					token = token.substring(1);
				} else if (token.startsWith("-")) {
					occur = Occur.MUST_NOT;
					token = token.substring(1);
				}
			}
			DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, occur, false);
			query.addClause(dmq);
			Term term = new Term(dmq, token);
			dmq.addClause(term);
		}
		return query;
	}

}
