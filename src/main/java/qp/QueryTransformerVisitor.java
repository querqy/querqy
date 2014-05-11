/**
 * 
 */
package qp;

import java.util.LinkedList;
import java.util.List;

import qp.model.BooleanQuery;
import qp.model.BooleanQuery.Operator;
import qp.model.DisjunctionMaxQuery;
import qp.model.Node;
import qp.model.Query;
import qp.model.SubQuery.Occur;
import qp.model.Term;
import qp.parser.QueryBaseVisitor;
import qp.parser.QueryParser.BooleanPrefixContext;
import qp.parser.QueryParser.BooleanQueryContext;
import qp.parser.QueryParser.ClauseContext;
import qp.parser.QueryParser.FieldNameContext;
import qp.parser.QueryParser.NoopQueryContext;
import qp.parser.QueryParser.OpAndContext;
import qp.parser.QueryParser.OpOrContext;
import qp.parser.QueryParser.QueryContext;
import qp.parser.QueryParser.TermContext;
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
		return super.visitBooleanPrefix(ctx);
	}
	
	@Override
	public Node visitNoopQuery(NoopQueryContext ctx) {

		BooleanQuery parent = booleanQueryStack.getLast();
		BooleanQuery query = new BooleanQuery(parent, Operator.NONE, occurBuffer);
		parent.addClause(query);
		
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
		
		BooleanQuery parent = booleanQueryStack.getLast();
		BooleanQuery query = new BooleanQuery(parent, op, occurBuffer);
		parent.addClause(query);
		
		booleanQueryStack.add(query);
		
		super.visitBooleanQuery(ctx);
		
		return booleanQueryStack.removeLast();
	}
	
	
	@Override
	public Node visitTermQuery(TermQueryContext ctx) {
		
		BooleanQuery parent = booleanQueryStack.getLast();
		
		DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(parent, occurBuffer);

		String text = ctx.getRuleContext(TermContext.class, 0).getText();

		
		
		List<FieldNameContext> fieldNameContexts = ctx.getRuleContexts(FieldNameContext.class);
		if (fieldNameContexts != null && !fieldNameContexts.isEmpty()) {
			for (FieldNameContext fieldNameContext: fieldNameContexts) {
				String fieldName = fieldNameContext.getText();
				dmq.addClause(new Term(dmq, fieldName, text));
			}
		} else {
			dmq.addClause(new Term(dmq, text));
		}
		
		
		
		parent.addClause(dmq);
		return dmq;
	}

}
