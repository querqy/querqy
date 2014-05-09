/**
 * 
 */
package qp;

import java.util.LinkedList;

import qp.model.BooleanQuery;
import qp.model.Node;
import qp.model.BooleanClause.Occur;
import qp.model.TermQuery;
import qp.parser.QueryBaseVisitor;
import qp.parser.QueryParser.BooleanClauseContext;
import qp.parser.QueryParser.BooleanPrefixContext;
import qp.parser.QueryParser.QueryContext;
import qp.parser.QueryParser.TermQueryContext;

/**
 * @author rene
 *
 */
public class QueryTransformerVisitor extends QueryBaseVisitor<Node> {
	
	Occur occurBuffer = Occur.SHOULD;
	
	LinkedList<BooleanQuery> booleanQueryStack = new LinkedList<>();
	
	@Override
	public Node visitBooleanPrefix(BooleanPrefixContext ctx) {
		String prf = ctx.getText();
		if (prf.length() == 1) {
			switch (prf.charAt(0)) {
			case '+' : occurBuffer = Occur.MUST; break;
			case '-' : occurBuffer = Occur.MUST_NOT; break;
			}
		}
		return occurBuffer;
	}
	
	@Override
	public Node visitBooleanClause(BooleanClauseContext ctx) {
		occurBuffer = Occur.SHOULD;
		return super.visitBooleanClause(ctx);
	}
	
	@Override
	public Node visitQuery(QueryContext ctx) {
		BooleanQuery bq = new BooleanQuery(occurBuffer);
		booleanQueryStack.add(bq);
		super.visitQuery(ctx);
		booleanQueryStack.removeLast();
		if (!booleanQueryStack.isEmpty()) {
			booleanQueryStack.getLast().addClause(bq);
		}
		return bq;
	}
	
	@Override
	public Node visitTermQuery(TermQueryContext ctx) {
		TermQuery tq = new TermQuery(ctx.getText(), occurBuffer);
		booleanQueryStack.getLast().addClause(tq);
		return tq;
	}

}
