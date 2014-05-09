/**
 * 
 */
package qp;

import java.util.LinkedList;

import qp.model.BooleanClause;
import qp.model.BooleanQuery;
import qp.model.BooleanClause.Occur;
import qp.model.TermQuery;
import qp.parser.QueryBaseListener;
import qp.parser.QueryParser.BooleanClauseContext;
import qp.parser.QueryParser.BooleanPrefixContext;
import qp.parser.QueryParser.QueryContext;
import qp.parser.QueryParser.TermQueryContext;

/**
 * @author rene
 *
 */
public class QueryTransformerListener extends QueryBaseListener {
	
	Occur occurBuffer = Occur.SHOULD;
	
	LinkedList<BooleanQuery> booleanQueryStack = new LinkedList<>();
	BooleanQuery mainQuery = null;
	
	public BooleanQuery getQuery() {
		return mainQuery;
	}
	
	@Override
	public void enterQuery(QueryContext ctx) {
		BooleanQuery newQuery = new BooleanQuery(occurBuffer);
		booleanQueryStack.add(newQuery);
	}
	
	@Override
	public void exitQuery(QueryContext ctx) {
		mainQuery = booleanQueryStack.removeLast();
		if (!booleanQueryStack.isEmpty()) {
			booleanQueryStack.getLast().addClause(mainQuery);
		}
	}
	
	@Override
	public void enterBooleanClause(BooleanClauseContext ctx) {
		occurBuffer = Occur.SHOULD;
	}
	
	@Override
	public void exitTermQuery(TermQueryContext ctx) {
		booleanQueryStack.getLast().addClause(new TermQuery(ctx.getText(), occurBuffer));
	}
	
	@Override
	public void exitBooleanPrefix(BooleanPrefixContext ctx) {
		String prf = ctx.getText();
		if (prf.length() == 1) {
			switch (prf.charAt(0)) {
			case '+' : occurBuffer = Occur.MUST; break;
			case '-' : occurBuffer = Occur.MUST_NOT; break;
			}
		}
	}
	
	
}
