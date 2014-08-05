/**
 * 
 */
package querqy.antlr;

import java.util.LinkedList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import querqy.antlr.parser.QueryBaseVisitor;
import querqy.antlr.parser.QueryParser.BooleanPrefixContext;
import querqy.antlr.parser.QueryParser.BooleanQueryContext;
import querqy.antlr.parser.QueryParser.ClauseContext;
import querqy.antlr.parser.QueryParser.FieldNameContext;
import querqy.antlr.parser.QueryParser.NoopQueryContext;
import querqy.antlr.parser.QueryParser.OpAndContext;
import querqy.antlr.parser.QueryParser.OpOrContext;
import querqy.antlr.parser.QueryParser.QueryContext;
import querqy.antlr.parser.QueryParser.TermContext;
import querqy.antlr.parser.QueryParser.TermQueryContext;
import querqy.model.BooleanQuery;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Node;
import querqy.model.Query;
import querqy.model.Term;
import querqy.model.SubQuery.Occur;

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
		BooleanQuery query = new BooleanQuery(parent, getOccur(), false);
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
		BooleanQuery query = new BooleanQuery(parent, getOccur(), false);
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
		
		DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(parent, getOccur(), false);
		
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

