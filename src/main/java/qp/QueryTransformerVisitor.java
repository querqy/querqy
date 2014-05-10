/**
 * 
 */
package qp;

import java.util.LinkedList;
import java.util.List;

import qp.model.BooleanQuery;
import qp.model.BooleanQuery.Operator;
import qp.model.Clause.Occur;
import qp.model.Node;
import qp.model.Query;
import qp.model.TermQuery;
import qp.parser.QueryBaseVisitor;
import qp.parser.QueryParser.BooleanPrefixContext;
import qp.parser.QueryParser.BooleanQueryContext;
import qp.parser.QueryParser.ClauseContext;
import qp.parser.QueryParser.NoopQueryContext;
import qp.parser.QueryParser.OpAndContext;
import qp.parser.QueryParser.OpOrContext;
import qp.parser.QueryParser.QueryContext;
import qp.parser.QueryParser.TermQueryContext;

/**
 * @author rene
 *
 */
public class QueryTransformerVisitor extends QueryBaseVisitor<Node> {
	
	LinkedList<BooleanQuery> booleanQueryStack = new LinkedList<>();
	
	Occur occurBuffer = Occur.SHOULD;
	
	@Override
	public Node visitQuery(QueryContext ctx) {
		Query query = new Query();
		booleanQueryStack.add(query);
		super.visitQuery(ctx);
		return booleanQueryStack.removeLast();
	}
	
	@Override
	public Node visitClause(ClauseContext ctx) {
		occurBuffer = Occur.SHOULD;
		return super.visitClause(ctx);
	}
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
	public Node visitNoopQuery(NoopQueryContext ctx) {
		BooleanQuery query = new BooleanQuery(Operator.NONE, occurBuffer);
		
		booleanQueryStack.getLast().addClause(query);
		booleanQueryStack.add(query);
		super.visitNoopQuery(ctx);
		return booleanQueryStack.removeLast();
	}
	
	@Override
	public Node visitBooleanQuery(BooleanQueryContext ctx) {
		Operator op = Operator.NONE;
		List<OpAndContext> and = ctx.getRuleContexts(OpAndContext.class);
		if (and != null && !and.isEmpty()) {
			op = Operator.AND;
		} else {
			List<OpOrContext> or = ctx.getRuleContexts(OpOrContext.class);
			if (or != null && !or.isEmpty()) {
				op = Operator.OR;
			}
		}
		
		BooleanQuery query = new BooleanQuery(op, occurBuffer);
		
		booleanQueryStack.getLast().addClause(query);
		booleanQueryStack.add(query);
		super.visitBooleanQuery(ctx);
		return booleanQueryStack.removeLast();
	}
	
	@Override
	public Node visitOpAnd(OpAndContext ctx) {
		return Operator.AND;
	}
	
	@Override
	public Node visitOpOr(OpOrContext ctx) {
		return Operator.OR;
	}
	
	@Override
	public Node visitTermQuery(TermQueryContext ctx) {
		TermQuery tq = new TermQuery(ctx.getText(), occurBuffer);
		booleanQueryStack.getLast().addClause(tq);
		return tq;
	}
	/*
	
	;
	
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
		Query bq = new Query(occurBuffer);
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
	}*/

}
