/**
 * 
 */
package qp;

import java.util.LinkedList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import qp.model.BooleanQuery;
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
    
    enum Operator { NONE, AND, OR;}

	
	LinkedList<BooleanQuery> booleanQueryStack = new LinkedList<>();
	LinkedList<Operator> operatorStack = new LinkedList<>();
	
	Occur occurBuffer = Occur.SHOULD;
	
	final char[] input;
	
	public QueryTransformerVisitor(char[] input) {
	    this.input = input;
	}
	
	@Override
	public Node visitQuery(QueryContext ctx) {
		Query query = new Query();
		operatorStack.add(Operator.NONE);
		booleanQueryStack.add(query);
		super.visitQuery(ctx);
		operatorStack.removeLast();
		return booleanQueryStack.removeLast();
	}
	
	@Override
	public Node visitClause(ClauseContext ctx) {
		occurBuffer = getOccur(ctx);
		Node result = super.visitClause(ctx);
		occurBuffer = Occur.SHOULD;
		return result;
	}

	
	@Override
	public Node visitNoopQuery(NoopQueryContext ctx) {

		BooleanQuery parent = booleanQueryStack.getLast();
		BooleanQuery query = new BooleanQuery(parent, getOccur());
		parent.addClause(query);
		
		operatorStack.add(Operator.NONE);
		booleanQueryStack.add(query);
		super.visitNoopQuery(ctx);
		operatorStack.removeLast();
		return booleanQueryStack.removeLast();
	}
	
	Occur getOccur(ParserRuleContext ctx) {
		List<BooleanPrefixContext> contexts = ctx.getRuleContexts(BooleanPrefixContext.class);
		if (contexts == null || contexts.isEmpty()) {
			return Occur.SHOULD;
		}
		
		String prf =contexts.get(0).getText();
		if (prf.length() == 1) {
			switch (prf.charAt(0)) {
			case '+' :return Occur.MUST; 
			case '-' : return Occur.MUST_NOT; 
			}
		} 
		return Occur.SHOULD;
		
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
		BooleanQuery query = new BooleanQuery(parent, getOccur());
		parent.addClause(query);
		
		operatorStack.add(op);
		booleanQueryStack.add(query);
		
		super.visitBooleanQuery(ctx);
		
		operatorStack.removeLast();
		return booleanQueryStack.removeLast();
	}
	
	Occur getOccur() {
	    
	    if (occurBuffer == Occur.SHOULD && operatorStack.getLast() == Operator.AND) {
	        
	        return Occur.MUST;
	        
	    } else {
	        
	        return occurBuffer;
	        
	    }
	}
	
	@Override
	public Node visitTermQuery(TermQueryContext ctx) {
		
		BooleanQuery parent = booleanQueryStack.getLast();
		
		DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(parent, getOccur());
		
		TermContext tc = ctx.getRuleContext(TermContext.class, 0);

		Token startToken = tc.getStart();
		
		List<FieldNameContext> fieldNameContexts = ctx.getRuleContexts(FieldNameContext.class);
		if (fieldNameContexts != null && !fieldNameContexts.isEmpty()) {
			for (FieldNameContext fieldNameContext: fieldNameContexts) {
				String fieldName = fieldNameContext.getText();
				dmq.addClause(new Term(dmq, fieldName, input, startToken.getStartIndex(), 1 + startToken.getStopIndex() - startToken.getStartIndex() ));
			}
		} else {
			dmq.addClause(new Term(dmq, input, startToken.getStartIndex(), 1 + startToken.getStopIndex() - startToken.getStartIndex()));
		}
		
		
		
		parent.addClause(dmq);
		return dmq;
	}

}

